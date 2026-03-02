## Context

- 项目：ShareMonitor（Android/Kotlin/Compose）。
- 目标场景：真机联调时快速安装 Debug 并启动入口 Activity。
- 约束：
  - 兼容 macOS 默认 Bash（3.x），避免 `mapfile` 等 Bash 4+ 特性。
  - 多设备连接时不做“猜测”，需要显式指定目标设备序列号。

## Goals / Non-Goals

**Goals:**

- 一条命令完成 Debug 安装与启动入口 Activity。
- 默认行为“够用”：自动推断 `applicationId` 与 LAUNCHER Activity。
- 允许覆盖：通过参数指定 serial / appId / activity。

**Non-Goals:**

- 不引入复杂的设备选择交互 UI。
- 不覆盖 Release / 多变体安装流程（本变更聚焦 Debug）。

## Decisions

1) **设备选择策略**

- 若 `ANDROID_SERIAL` 或 `--serial` 提供，则使用该设备。
- 未提供时：
  - 只有 1 台 `device` 状态设备则自动选择
  - 多台设备则报错并提示使用 `--serial`

2) **applicationId 推断**

- 默认从 `app/build.gradle.kts` 中读取 `applicationId = "..."`。
- 无法解析时要求传 `--app-id`。

3) **入口 Activity 推断**

- 默认从 `app/src/main/AndroidManifest.xml` 找到同时包含 `MAIN` + `LAUNCHER` 的 `activity`/`activity-alias` 的 `android:name`。
- 无法解析时要求传 `--activity`。

## Risks / Trade-offs

- [风险] Manifest/Gradle 格式变化导致解析失败 → [缓解] 提供 `--app-id`/`--activity` 覆盖参数。
- [取舍] 不做交互式多设备选择 → [影响] 多设备时需要用户明确指定 serial，但行为可预测、避免误刷错机。

