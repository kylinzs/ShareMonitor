## 1. 数据层修复（东方财富 / 仓储）

- [x] 1.1 在 `EastmoneyQuotesDataSource.getQuote()` 中，当 `lastPrice == null` 时返回 `null`（避免写入“空报价”）
- [x] 1.2 使用统一的 `QuoteMath` 计算 `change`/`changePct`（不在数据源内做 0.0 兜底差值）
- [x] 1.3 在 `QuotesRepository.refreshQuotes()` 中，当数据源返回空 quotes 列表时视为失败，并提供用户可见消息
- [x] 1.4 将 Moshi/数字解析异常映射为 `DataError.Parse`，便于 UI 提示“数据解析失败”

## 2. 详情页兜底与提示

- [x] 2.1 在 `DetailViewModel` 中将 `lastPrice == null` 的 Quote 视为不可用，允许回退逻辑接管
- [x] 2.2 当报价不可用但分时历史存在时，从分时 points 推导 `last/open/high/low` 作为兜底 Quote
- [x] 2.3 当使用分时推导兜底时，展示用户可见提示（例如“报价不可用，已从分时推导”）

## 3. 验证

- [x] 3.1 运行 `:app:compileDebugKotlin` 与 `:app:testDebugUnitTest`
- [x] 3.2 真机验证：设置数据源为“东方财富”，进入个股详情页确认核心字段不再长期为 `--`，且异常场景有提示/兜底展示
