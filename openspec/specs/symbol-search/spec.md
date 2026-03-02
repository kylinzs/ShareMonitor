# symbol-search Specification

## Purpose
定义 A 股代码/名称搜索能力，包括结果展示与可识别性、加入自选流程，以及空结果和失败的用户可见反馈。
## Requirements
### Requirement: 用户可搜索 A 股标的
系统应当（SHALL）允许用户按股票代码前缀与名称子串搜索 A 股标的。

#### Scenario: 搜索返回匹配标的
- **WHEN** 用户输入查询并触发搜索
- **THEN** 系统展示匹配标的列表，包含 `exchange`、`code` 与 `name`

#### Scenario: 搜索无结果
- **WHEN** 用户搜索的查询没有匹配标的
- **THEN** 系统展示“无匹配结果”的空状态

### Requirement: 搜索结果清晰可识别
系统应当（SHALL）为每个搜索结果展示交易所标识，并展示由 `exchange + code` 组成的唯一标识。

#### Scenario: 结果条目展示交易所与代码
- **WHEN** 系统渲染搜索结果条目
- **THEN** 条目展示交易所标识，并在名称旁展示代码

### Requirement: 用户可将搜索到的标的加入自选
系统应当（SHALL）允许用户从搜索结果中将标的加入自选列表。

#### Scenario: 从搜索结果加入自选成功
- **WHEN** 用户对某个搜索结果点击“Add”（添加）
- **THEN** 该标的被加入自选列表，UI 反映更新后的状态

### Requirement: 搜索失败对用户可见
如果搜索操作失败，系统必须（MUST）告知用户并允许重试。

#### Scenario: 搜索时发生网络错误
- **WHEN** 标的搜索因网络错误而失败
- **THEN** UI 展示错误信息与重试操作
