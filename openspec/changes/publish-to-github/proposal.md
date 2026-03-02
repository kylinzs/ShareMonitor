## Why

当前项目尚未托管到远程版本库，协作、备份与变更追踪成本高。将项目发布到 GitHub 能提供稳定的版本管理与远程备份，并便于后续 CI/协作开发。

## What Changes

- 初始化并规范化 Git 仓库（分支命名为 `main`），完成首次提交。
- 补齐/强化 `.gitignore`，避免将本地配置与构建产物（如 `local.properties`、`**/build`、`*.apk`）提交到仓库。
- 创建 GitHub 远程仓库并推送代码（HTTPS 或 SSH）。

## Capabilities

### New Capabilities
- `github-repo-publish`: 将项目以可复现、无敏感信息泄露的方式发布到 GitHub（包含 git 初始化、忽略规则、远程仓库推送流程）。

### Modified Capabilities
- （无；`openspec/specs/` 当前为空）

## Impact

- 影响范围：
  - 仓库根目录新增/更新 `.gitignore` 与 `.git/`（本地）
  - 新增 Git 历史（首次提交）
  - 新增 GitHub 远程仓库与 `origin` remote（需要网络与账号权限）
- 风险：
  - 误提交本地配置/缓存 → 通过 `.gitignore` 与提交前检查避免。

