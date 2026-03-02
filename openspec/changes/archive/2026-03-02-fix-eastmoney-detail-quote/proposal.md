## Why

东方财富数据源在部分情况下会返回“结构存在但核心字段为空”的报价数据，导致个股详情页只显示占位符（`--`），并且由于没有触发显式错误，用户无法判断是网络问题、接口变更还是数据源受限。

## What Changes

- 当东方财富返回的报价缺少关键字段（例如 `lastPrice` 为空）时，视为“无有效报价”，避免将“空报价”写入缓存导致 UI 静默空白。
- 报价刷新若返回空结果（例如 `getQuotes` 返回空列表），将其视为失败并给出用户可见提示。
- 在个股详情页，当报价不可用但分时历史存在时，从分时序列推导 `last/open/high/low` 作为兜底展示，并提示“报价不可用，已从分时推导”。

## Capabilities

### New Capabilities
- `stock-details-quote-fallback`: 详情页在“报价缺失/不完整”场景下的兜底展示与提示规范（不再静默只显示占位符）。
- `quote-refresh-failure-on-empty`: 当报价刷新拿到空结果时，将其视为失败并产出可提示的错误信息。

### Modified Capabilities
- （无；当前 `openspec/specs/` 为空，先以新增能力规格化本修复行为）

## Impact

- 受影响代码：
  - `app/src/main/java/com/codex/sharemonitor/data/quotes/eastmoney/EastmoneyQuotesDataSource.kt`
  - `app/src/main/java/com/codex/sharemonitor/data/quotes/QuotesRepository.kt`
  - `app/src/main/java/com/codex/sharemonitor/ui/viewmodel/DetailViewModel.kt`
- 用户体验：
  - 东方财富数据源异常时，不再出现“字段全空但无提示”的详情页；
  - 在报价不可用但分时数据存在时，仍可展示核心价格信息作为兜底。

