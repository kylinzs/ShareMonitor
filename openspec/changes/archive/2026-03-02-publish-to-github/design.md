## Context

- 项目：ShareMonitor（Android/Kotlin/Compose）。
- 目标：将项目托管到 GitHub，支持远程备份与后续协作。
- 约束：
  - 避免提交本地配置/缓存/构建产物（例如 `local.properties`、`**/build`、`*.apk`）。
  - 推送依赖 GitHub 账号权限与网络环境；若本机未配置 SSH Key 或 Token，需要先完成配置。

## Goals / Non-Goals

**Goals:**

- 在本地完成 Git 初始化、分支规范化（`main`）、首次提交。
- 明确远程仓库创建与推送步骤（HTTPS/SSH），保证流程可复现。
- 提供“推送前检查清单”，降低误提交风险。

**Non-Goals:**

- 不引入 CI/CD（GitHub Actions）或发布流水线；如需可另立 change。
- 不对代码结构/功能做任何额外改造，仅处理版本管理与托管。

## Decisions

1) **忽略规则优先于“推送后再补救”**

- 在首次提交前强化 `.gitignore`，并在提交前检查 staged 文件。
- 备选：先推送再删除敏感文件。缺点是历史已泄露且很难彻底清理；不采纳。

2) **分支命名使用 `main`**

- GitHub 默认与多数团队习惯一致，减少后续配置成本。

3) **远程创建方式：手动创建优先，CLI 作为可选**

- 若已安装并登录 `gh`，可用 `gh repo create` 一键创建。
- 否则手动在 GitHub 网页创建仓库，再 `git remote add origin ...` 推送即可。

## Risks / Trade-offs

- [风险] 误提交本地配置/秘钥文件 → 缓解：`.gitignore` + `git status`/`git diff --cached` 提交前检查。
- [风险] 远程推送失败（网络/认证）→ 缓解：同时提供 HTTPS 与 SSH 两种方式，并给出最小排障步骤。

