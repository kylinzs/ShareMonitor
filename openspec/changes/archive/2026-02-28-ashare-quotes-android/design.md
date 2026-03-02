## Context

- 项目现状：单模块 `:app`，仅包含最小 Android 依赖（AppCompat/Material），暂无 Kotlin/Compose、网络层、数据层与页面结构。
- 目标用户：希望快速查看 A 股自选股行情与个股详情的用户。
- 关键约束：
  - 行情数据源存在不确定性（可用性、限频、协议/合规、字段差异）。首版需要可插拔数据源与 Mock 能力，避免被单一 API 阻塞。
  - MVP 优先：先跑通核心闭环（搜索→加入自选→看行情→看详情），再逐步增强（K 线、提醒、更多榜单等）。

## Goals / Non-Goals

**Goals:**
- 使用现代 Android 技术栈实现可扩展的架构（MVVM + Repository + 可替换数据源）。
- 提供核心页面：自选列表、搜索/添加、个股详情（含分时线与日 K）、指数/板块入口（主要指数与板块列表/成分股）。
- 自选与设置可离线持久化；无网时展示上次缓存的数据并提示“数据可能过期”。
- 数据获取具备基础健壮性：错误分类提示、退避重试、客户端限频/合并请求策略；首版以“延迟约 15 分钟”更新策略为默认。

**Non-Goals:**
- 不在 MVP 里实现交易/下单、登录体系、云同步、多设备同步。
- 不在 MVP 里实现完整专业行情功能（Level-2、盘口深度、财务报表、研报、新闻流等）。
- 不承诺某个具体第三方行情供应商的长期可用性；生产接入需由后续明确供应商与授权方式。

## Decisions

1) UI：Jetpack Compose + Material 3
- Why：更快迭代 UI、状态驱动、与 MVVM 组合自然；更适合从 0→1 构建。
- Alternatives：
  - XML + RecyclerView：成熟但样板代码多、迭代慢；不利于快速试错。

2) 语言与架构：Kotlin + MVVM（ViewModel）+ 分层（ui / domain / data）
- Why：更清晰的边界，方便替换数据源与单测；后续扩展能力（提醒、更多页面）成本更低。
- Alternatives：
  - “Activity 直连网络”：短期快但不可维护，无法支撑后续扩展。

3) 数据源抽象：`QuotesDataSource`（接口）+ 多实现（`MockQuotesDataSource` / `VendorQuotesDataSource`）
- Why：行情供应商与字段高度不稳定；用接口隔离能保证 UI/业务先落地。
- Implementation sketch：
  - `getQuotes(symbols)`：批量报价（自选列表用）。
  - `getQuote(symbol)`：单股详情报价。
  - `searchSymbols(query)`：搜索与标的元数据。
  - `getHistory(symbol, range, interval)`：详情走势/历史（MVP 可先做日线或分时的一种）。
 - Vendor choices (MVP)：支持多个公开来源并允许用户切换，默认东方财富，备用腾讯（优先使用 HTTPS；如需 HTTP，则仅对东方财富/腾讯的指定域名放开明文流量并做域名白名单限制）。
 - Capability gating：仅将满足 MVP 能力集合（search/quotes/history intraday+daily/indices+sectors）的来源暴露给用户选择，避免“选了某来源某功能不可用”的体验。

4) 网络与序列化：Retrofit + OkHttp + kotlinx.serialization（或 Moshi）
- Why：成熟、可观测（日志/拦截器/缓存），并易于按供应商实现不同 API。
- Alternatives：
  - 直接用 `HttpURLConnection`：实现成本与可维护性差。

5) 本地持久化：Room（自选列表）+ DataStore（设置）
- Why：自选需要排序/置顶等结构化查询；设置适合 key-value。
- Alternatives：
  - 仅 SharedPreferences：可用但类型不安全、迁移与测试较弱。

6) 缓存与刷新策略：Repository 内做“内存 + 本地”两级缓存，UI 以手动刷新为主
- Why：无自建服务端且依赖公开接口时，稳定性与限频风险更高；默认 15 分钟刷新可显著降低失败率与被限流概率，同时保证启动速度与离线可用。
- Implementation sketch：在 Repository 中强制最小刷新间隔（默认 15 分钟，可在设置中调整），并对用户的“手动刷新”做节流与提示（例如“距离下次更新还有 X 分钟，当前展示为上次数据”）。
- 后续扩展点：若未来需要更高频或更稳定的数据，可切换为自建中转服务或授权供应商数据源。

7) 走势/图表：MVP 先实现轻量“迷你走势/简单 K 线”
- Why：避免引入重量级图表库带来的体积与适配成本；先满足“可视化趋势”。
- Scope (MVP)：同时支持分时线（当日分时）与日 K（回溯 6 个月），刷新频率与报价一致（默认最小 15 分钟）。
- Alternatives：
  - MPAndroidChart 等第三方库：功能强但学习/适配/主题成本更高。

8) 指数/板块入口（MVP）：聚合列表 + 板块成分股
- Why：自用场景常见需求是快速扫一眼大盘/热点；通过“指数/板块”入口补齐使用路径，并可从板块成分股快速加入自选。
- Scope (MVP)：
  - 主要指数列表（数据源提供的默认集合即可，避免本地硬编码过多）。
  - 板块列表（按涨跌幅/热度等排序由数据源决定，App 侧只提供展示与刷新）。
  - 板块详情：成分股列表（可点击进个股详情、可一键加入自选）。
  - 刷新频率：与行情一致，默认最小 15 分钟，并复用同一套缓存/节流策略。
 - Navigation label：在主导航中命名为“行情”。

## Risks / Trade-offs

- [行情数据源不可用或字段变化频繁] → 通过 `QuotesDataSource` 抽象 + Mock 实现保证开发推进；生产接入前增加契约测试与字段映射层。
- [限频导致请求失败/被封] → 批量接口优先、合并请求、指数退避重试；在 UI 上提供刷新节流与“上次更新时间”。
- [公开接口的条款/合规与长期可用性不确定] → 明确“自用”定位；在设置页展示数据来源与免责声明；保持可替换数据源以便快速切换。
- [合规与授权风险] → 设计上不绑定具体供应商；落地生产前必须明确授权方式与使用条款，并在 App 内加入免责声明/数据来源说明。
- [离线缓存导致数据过期误导] → 展示“更新时间”和“数据可能过期”的状态；超过阈值标记为过期并弱化展示。

## Migration Plan

1. 引入 Kotlin/Compose 与基础架构骨架（导航、主题、页面容器）。
2. 先接入 `MockQuotesDataSource` 跑通完整 UI 闭环。
3. 引入本地存储（Room/DataStore）与自选/设置持久化。
4. 接入真实行情数据源实现（可配置切换），完善错误处理与限频策略。
5. 增加基础测试（Repository/映射层单测，关键 UI 流程冒烟测试）。

## Open Questions

- 除东方财富外，首版备用公开来源选哪家（以及端点/字段映射/稳定性策略）？
