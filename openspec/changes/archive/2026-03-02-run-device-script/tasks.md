## 1. 新增脚本

- [x] 1.1 新增 `scripts/run_device.sh`，支持安装 Debug + 启动入口 Activity
- [x] 1.2 支持 `--serial`/`ANDROID_SERIAL` 多设备场景
- [x] 1.3 支持 `--no-install` 仅启动
- [x] 1.4 自动推断 `applicationId` 与 LAUNCHER Activity，并支持 `--app-id`/`--activity` 覆盖

## 2. 兼容性与验证

- [x] 2.1 兼容 macOS 默认 Bash（移除 `mapfile` 依赖）
- [x] 2.2 确认脚本具备可执行权限

