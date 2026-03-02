## 1. 代码规范文档

- [x] 1.1 新增 `CODE_STYLE.md`（或 `docs/code-style.md`）并写明“新增代码文件必须包含中文注释”的规则与最小要求
- [x] 1.2 在规范中补充示例（文件级 KDoc、public API KDoc、避免噪音注释的示例）
- [x] 1.3 在规范中加入 code review 检查项（新增文件缺少中文注释则不得合入）

## 2. Domain Model 中文注释

- [x] 2.1 为 `domain/model` 下核心模型补齐中文 KDoc（含关键字段解释）
- [x] 2.2 统一术语与表达（如“行情/报价/缓存/刷新/节流/回退”等）并做一次快速自查

## 3. Data 层中文注释

- [x] 3.1 为 `QuotesRepository` 增加中文 KDoc（缓存、刷新策略、节流/最小间隔、数据源回退等）
- [x] 3.2 为 `QuotesDataSource` 与各实现（Eastmoney/Tencent/Mock）补充中文 KDoc（职责、字段归一化、已知差异）
- [x] 3.3 为 `HttpClientFactory`/网络拦截器规则补充中文说明（UA/Referer 目的与影响）
- [x] 3.4 为 `SettingsRepository`/`WatchlistRepository` 补充中文 KDoc（持久化、默认值、去重/排序策略）
- [x] 3.5 为 `AppContainer` 补充中文 KDoc（依赖注入/实例生命周期/调试开关）

## 4. UI/ViewModel 中文注释

- [x] 4.1 为各 `ViewModel`（Watchlist/Search/Detail/Market/SectorDetail）补充中文 KDoc（状态含义、主要动作、非直观逻辑）
- [x] 4.2 为导航入口 `AppScaffold`/`MainActivity`/`ShareMonitorApp` 增加中文 KDoc（路由与职责边界）
- [x] 4.3 对少量关键 UI 逻辑增加必要行内注释（仅限回退/节流/缓存/异常提示等非显而易见部分）

## 5. 验证与收尾

- [x] 5.1 运行 `:app:compileDebugKotlin` 确保注释/导入不引入编译错误
- [x] 5.2 运行 `:app:testDebugUnitTest` 确保单元测试通过
- [x] 5.3 连接真机运行 `:app:connectedDebugAndroidTest`（或最小 smoke）确保基础流程不受影响
