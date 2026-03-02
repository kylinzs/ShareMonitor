## 1. 项目初始化

- [x] 1.1 添加 Kotlin Android 插件与 Kotlin toolchain 配置
- [x] 1.2 启用 Jetpack Compose + Material 3，并配置 Compose 编译器选项
- [x] 1.3 添加核心依赖（Lifecycle/ViewModel、Navigation、Coroutines）
- [x] 1.4 添加网络依赖（OkHttp、Retrofit、Moshi/序列化），并在 Debug 下启用日志输出
- [x] 1.5 添加持久化依赖（Room、DataStore），并按需配置 kapt/ksp
- [x] 1.6 添加 `INTERNET` 权限，并创建最小可运行的应用入口页面

## 2. 领域模型（Domain Models）

- [x] 2.1 定义 `Exchange` 与 `Symbol`（exchange + code + name）模型
- [x] 2.2 定义归一化的 `Quote` 模型（包含“报价新鲜度/更新时间”等元数据）
- [x] 2.3 定义 `HistoryPoint` 与历史类型（Intraday、DailyK），以及范围/粒度模型（DailyK：6 个月）
- [x] 2.4 定义面向 UI 的错误模型（网络、解析、限流、未知）
- [x] 2.5 定义 `Index` 与 `Sector` 模型，以及最小的成分股条目模型

## 3. 设置（Settings / DataStore）

- [x] 3.1 建立设置 schema：数据源选择 + 自动刷新配置（默认间隔：15 分钟）
- [x] 3.2 实现 SettingsRepository，用于读取/写入设置
- [x] 3.3 为设置页补齐 UI 状态映射（包括免责声明 + 已选择的数据源名称）
- [x] 3.4 确保只有支持 MVP 能力的数据源可被用户选择

## 4. 自选股持久化（Watchlist / Room）

- [x] 4.1 为自选股条目创建 Room Entity（唯一键：exchange + code）
- [x] 4.2 增加排序与置顶字段，并为未来变更制定迁移策略
- [x] 4.3 实现 WatchlistDao（列表、若不存在则插入、删除、重排、置顶/取消置顶）
- [x] 4.4 实现 WatchlistRepository，并暴露可观察/响应式的自选列表流

## 5. 行情数据源（Quote Data Sources）

- [x] 5.1 定义 `QuotesDataSource` 接口（搜索、批量报价、单个报价、历史数据）
- [x] 5.2 实现 `MockQuotesDataSource`（可复现的样例数据：symbols/quotes/history）
- [x] 5.3 接入东方财富（Eastmoney）数据源：Retrofit Service + DTO 映射层
- [x] 5.4 配置网络安全策略：若必须使用 HTTP，仅对指定 Eastmoney 域名放开明文（优先 HTTPS）
- [x] 5.5 接入腾讯（Tencent）数据源：Retrofit Service + DTO 映射层
- [x] 5.6 配置网络安全策略：若必须使用 HTTP，仅对指定 Tencent 域名放开明文（优先 HTTPS）
- [x] 5.7 在 `QuotesRepository` 中实现“通过设置切换数据源”，并覆盖所有功能入口
- [x] 5.8 实现 Eastmoney 历史：Intraday（今日）与 DailyK（6 个月）拉取，并做容错解析
- [x] 5.9 实现 Eastmoney 指数列表、板块列表、板块成分股拉取，并做容错解析
- [x] 5.10 实现 Tencent 历史：Intraday（今日）与 DailyK（6 个月）拉取，并做容错解析
- [x] 5.11 无论报价提供方如何，“行情”中的指数/板块固定使用 Eastmoney

## 6. 拉取、缓存与限流（Quote Fetching / Caching / Rate Limiting）

- [x] 6.1 增加内存缓存：按 Symbol 缓存最新报价
- [x] 6.2 增加持久化缓存（可选），或复用“上一次已知值 + 时间戳”
- [x] 6.3 实现最小刷新间隔约束（默认 15 分钟），并给出用户可见提示
- [x] 6.4 实现客户端侧的刷新突发节流/限频（按 specs）
- [x] 6.5 实现“过期/陈旧”阈值检测，并传播到 UI 状态
- [x] 6.6 为瞬时失败实现带退避的重试策略

## 7. UI 导航骨架（UI Navigation Shell）

- [x] 7.1 配置 Compose 导航路由：Watchlist、Search、Detail、Settings
- [x] 7.2 实现应用主题、字体与基础 Scaffold 布局
- [x] 7.3 增加跨页面共享的顶层错误处理/Snackbar 机制
- [x] 7.4 增加“行情”导航入口（指数/板块页面）

## 8. 自选股列表页（Watchlist Screen）

- [x] 8.1 实现 WatchlistViewModel：合并自选列表 + 报价流 + 新鲜度状态
- [x] 8.2 渲染自选条目（报价字段 + “最后更新”时间戳）
- [x] 8.3 实现下拉刷新与节流反馈
- [x] 8.4 实现删除、重排、置顶/取消置顶交互（持久化）
- [x] 8.5 实现空态（无自选）并提供跳转搜索/添加的 CTA

## 9. 标的搜索页（Symbol Search Screen）

- [x] 9.1 实现 SearchViewModel：带防抖的查询 + 可重试的错误处理
- [x] 9.2 渲染搜索结果（交易所标识 + 代码 + 名称，按 specs）
- [x] 9.3 支持从搜索结果加入自选，并对重复加入给出反馈
- [x] 9.4 实现无结果状态与加载指示

## 10. 个股详情页（Stock Detail Screen）

- [x] 10.1 实现 DetailViewModel：加载所选标的的 quote + history
- [x] 10.2 渲染核心报价字段（price/change/open/high/low/volume/time）
- [x] 10.3 详情页增加历史模式切换（Intraday / DailyK）
- [x] 10.4 基于 `HistoryPoint` 渲染 MVP 版分时折线图
- [x] 10.5 基于 `HistoryPoint` 渲染近 6 个月的 MVP 日 K 图
- [x] 10.6 详情页实现刷新（quote + history），并遵守最小刷新间隔约束
- [x] 10.7 详情页实现“加入/移出自选”开关
- [x] 10.8 若今日非交易/未开盘，则在详情展示上一交易日收盘价

## 11. 设置页（Settings Screen）

- [x] 11.1 实现设置 UI：数据源选择 + 自动刷新控制
- [x] 11.2 展示免责声明与已选择的数据源名称（按 specs）
- [x] 11.3 将设置 UI 与 SettingsRepository 打通，并验证持久化正确

## 12. 质量、测试与打磨（Quality / Testing / Polish）

- [x] 12.1 为报价归一化、节流与过期检测添加单元测试
- [x] 12.2 为自选唯一性与排序添加基础 DAO 测试（或 Repository 测试）
- [x] 12.3 为导航与空态/错误态添加简单的 UI 冒烟测试
- [x] 12.4 增加基础无障碍标签，并确保关键 UI 在大字体下可用
- [x] 12.5 验证真机 Instrumentation 测试仍能通过

## 13. 指数 / 板块（Indices / Sectors）

- [x] 13.1 增加“行情”页面：Tab（Indices / Sectors）
- [x] 13.2 实现 IndicesViewModel，并渲染指数列表（含报价与最后更新时间）
- [x] 13.3 实现 SectorsViewModel，并渲染板块列表与排序（至少按 changePct）
- [x] 13.4 实现 SectorDetail 页面，展示板块成分股列表
- [x] 13.5 支持从板块成分股加入自选，并对重复加入给出反馈
- [x] 13.6 为指数/板块页面实现刷新 + 最小刷新间隔约束
