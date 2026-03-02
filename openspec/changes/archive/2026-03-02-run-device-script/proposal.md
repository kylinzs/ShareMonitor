## Why

当前项目在“真机联调/冒烟验证”时，需要手动执行多条命令（连接设备、安装 Debug、再启动入口 Activity）。这类操作高频且容易出错（多设备选择、包名/Activity 拼写、是否需要重装等），会增加日常开发成本。

## What Changes

- 新增 `scripts/run_device.sh`：一条命令完成 `:app:installDebug` + 启动 LAUNCHER Activity。
- 脚本支持：
  - 单设备自动选择；多设备时通过 `--serial`/`ANDROID_SERIAL` 指定
  - `--no-install` 跳过安装，仅启动
  - `--app-id`/`--activity` 手动覆盖（默认从 Gradle/Manifest 推断）

## Capabilities

### New Capabilities

- `dev-run-on-device`: 开发者可以用脚本快速把 Debug 包装到真机并启动入口页面。

### Modified Capabilities

- （无）

## Impact

- 新增文件：`scripts/run_device.sh`
- 不改变 App 运行逻辑，仅影响本地开发/调试流程。

