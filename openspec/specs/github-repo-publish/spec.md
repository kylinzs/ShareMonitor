# github-repo-publish Specification

## Purpose
将 ShareMonitor 项目以可复现、可审核的方式发布到 GitHub，并确保仓库不包含本地配置与构建产物。
## Requirements
### Requirement: 仓库必须（MUST）排除仅本地配置与构建产物
仓库必须（MUST）避免提交仅本地使用的配置与生成的构建产物，包括但不限于 `local.properties`、`**/build` 以及打包产物（如 `*.apk`）。

#### Scenario: 首次提交暂存不包含本地/构建产物
- **WHEN** 用户为首次提交执行暂存（stage）操作
- **THEN** 暂存区不包含仅本地配置与构建产物

### Requirement: 仓库应当（SHALL）提供可复现的 GitHub 发布工作流
系统应当（SHALL）定义一套可复现的工作流将项目发布到 GitHub，包括初始化 git、创建首次提交、将默认分支配置为 `main`，并推送到远程 `origin`。

#### Scenario: 通过 HTTPS 发布到 GitHub
- **WHEN** 用户创建 GitHub 仓库并使用 HTTPS URL 设置 `origin`
- **THEN** 用户可以成功将 `main` 分支推送到 GitHub

#### Scenario: 通过 SSH 发布到 GitHub
- **WHEN** 用户创建 GitHub 仓库并使用 SSH URL 设置 `origin`
- **THEN** 用户可以成功将 `main` 分支推送到 GitHub
