# 代码规范（ShareMonitor）

本文档用于统一本项目的代码风格与约定，降低维护成本与误改风险。

## 1. 中文注释（强制）

**规则：以后新增的代码文件必须包含中文注释。**

最小要求（适用于 Kotlin/Java）：

1) **文件级用途说明（KDoc/Javadoc）**
- 新增的 Kotlin 文件，应在 `package` 之前添加文件级 KDoc，说明“本文件做什么/包含什么核心概念/边界是什么”。

示例（Kotlin）：

```kotlin
/**
 * 搜索模块：负责股票代码/名称的查询，以及失败回退策略的提示。
 */
package com.codex.sharemonitor.ui.screens
```

2) **对外可见 API 的中文 KDoc**
- `public` 的 class/interface/object、以及跨层调用的重要函数（如 Repository/DataSource/ViewModel 的关键入口）必须有中文 KDoc。
- 注释应描述**意图/职责/关键约束**，避免“翻译代码”或重复显而易见的逻辑。

3) **行内注释只写“非显而易见”的点**
- 仅在以下场景添加少量行内注释：
  - 缓存/节流/最小刷新间隔等时序策略
  - 数据源回退与兜底逻辑
  - 兼容性处理（字段别名、解析容错、请求头要求等）
  - UI 的异常/空态提示策略

## 2. 命名与结构（约定）

- **分层清晰**：`domain`（模型/规则）→ `data`（网络/本地/仓储）→ `ui`（ViewModel/Screen）。
- **命名一致**：优先使用业务术语：`行情/报价`、`缓存`、`刷新`、`节流`、`回退`。
- **避免无意义缩写**：除非是行业通用缩写（如 `K` 线、`DTO`）。

## 3. Code Review 检查项（必须检查）

涉及新增文件或新增 public API 时，评审必须确认：

- [ ] 新增文件包含中文用途说明（文件级 KDoc/Javadoc）
- [ ] 新增/修改的 public API 有中文 KDoc（职责、参数、返回、边界）
- [ ] 行内注释仅用于非显而易见逻辑，无噪音式注释
- [ ] 变更后可编译，并通过基础测试（至少 `:app:compileDebugKotlin` 与 `:app:testDebugUnitTest`）

