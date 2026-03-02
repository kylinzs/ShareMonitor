# cn-code-comments Specification

## Purpose
定义仓库中文 KDoc/注释规范：核心类型需提供中文文档注释，避免噪音注释，并保持注释与实际行为一致以减少漂移。
## Requirements
### Requirement: 核心 Kotlin 类型提供中文 KDoc
代码库应当（SHALL）为核心 Kotlin 类型提供中文 KDoc，使读者无需反向推导实现细节即可理解意图、使用边界与关键假设。

#### Scenario: 领域模型有中文文档
- **WHEN** 开发者打开 `app/src/main/java/**/domain/model/` 下的领域模型文件
- **THEN** 文件中的主要类型与关键字段包含中文 KDoc，说明用途与字段含义

#### Scenario: 数据层有中文文档
- **WHEN** 开发者打开 `app/src/main/java/**/data/` 下的仓库/数据源代码
- **THEN** 仓库与数据源包含中文 KDoc，说明职责、缓存/刷新行为与重要约束

#### Scenario: 视图模型（ViewModel）有中文文档
- **WHEN** 开发者打开 `app/src/main/java/**/ui/viewmodel/` 下的 ViewModel 代码
- **THEN** 每个 ViewModel 包含中文 KDoc，说明 UI 状态、主要操作与非显而易见的逻辑（如限流、回退）

### Requirement: 注释避免噪音并与行为一致
代码库应当（SHALL）避免冗余的“翻译式”注释，并且应当（SHALL）让注释聚焦于稳定的意图与契约，以减少漂移。

#### Scenario: 明显的 UI 布局不过度注释
- **WHEN** 开发者评审 UI Composable
- **THEN** 不为显而易见的布局代码添加注释，只记录非显而易见的行为/假设
