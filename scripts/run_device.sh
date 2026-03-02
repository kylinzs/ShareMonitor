#!/usr/bin/env bash
#
# 运行脚本：安装 Debug APK 到真机并启动主入口 Activity。
# - 默认自动选择唯一已连接设备；多设备时需指定 --serial 或设置 ANDROID_SERIAL
# - 默认从 app/build.gradle.kts 读取 applicationId，从 AndroidManifest.xml 推断 LAUNCHER Activity
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_GRADLE="$PROJECT_ROOT/app/build.gradle.kts"
MANIFEST="$PROJECT_ROOT/app/src/main/AndroidManifest.xml"

usage() {
  cat <<'EOF'
Usage:
  scripts/run_device.sh [--serial <deviceSerial>] [--app-id <applicationId>] [--activity <activityName>] [--no-install]

Examples:
  scripts/run_device.sh
  scripts/run_device.sh --serial b0077151
  scripts/run_device.sh --app-id com.codex.sharemonitor --activity .MainActivity
EOF
}

SERIAL="${ANDROID_SERIAL:-}"
APP_ID=""
ACTIVITY=""
DO_INSTALL=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --serial)
      SERIAL="${2:-}"
      shift 2
      ;;
    --app-id)
      APP_ID="${2:-}"
      shift 2
      ;;
    --activity)
      ACTIVITY="${2:-}"
      shift 2
      ;;
    --no-install)
      DO_INSTALL=0
      shift 1
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing file: $path" >&2
    exit 1
  fi
}

detect_adb() {
  if [[ -n "${ADB:-}" ]] && [[ -x "${ADB:-}" ]]; then
    echo "$ADB"
    return 0
  fi

  # 优先使用项目的 Android SDK（避免 PATH 上的 adb 版本不一致导致问题）。
  if [[ -f "$PROJECT_ROOT/local.properties" ]]; then
    local sdk_dir_from_properties
    sdk_dir_from_properties="$(awk -F= '$1=="sdk.dir"{sub(/^[ \t]+/, "", $2); print $2}' "$PROJECT_ROOT/local.properties" | tail -n 1)"
    if [[ -n "$sdk_dir_from_properties" ]] && [[ -x "$sdk_dir_from_properties/platform-tools/adb" ]]; then
      echo "$sdk_dir_from_properties/platform-tools/adb"
      return 0
    fi
  fi

  local sdk_dir=""
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    sdk_dir="$ANDROID_SDK_ROOT"
  elif [[ -n "${ANDROID_HOME:-}" ]]; then
    sdk_dir="$ANDROID_HOME"
  fi

  if [[ -n "$sdk_dir" ]] && [[ -x "$sdk_dir/platform-tools/adb" ]]; then
    echo "$sdk_dir/platform-tools/adb"
    return 0
  fi

  if command -v adb >/dev/null 2>&1; then
    command -v adb
    return 0
  fi

  echo "adb not found. Put it in PATH, set ADB, or set ANDROID_SDK_ROOT/ANDROID_HOME." >&2
  exit 1
}

pick_serial() {
  local adb_path="$1"
  if [[ -n "$SERIAL" ]]; then
    echo "$SERIAL"
    return 0
  fi

  local adb_output
  local server_error_regex='failed to check server version|cannot connect to daemon|ADB server didn'"'"'t ACK|could not install \*smartsocket\* listener'

  adb_output="$("$adb_path" devices -l 2>&1 || true)"
  if echo "$adb_output" | grep -qE "$server_error_regex"; then
    # 尝试自动重启 adb server 一次，避免偶发的 daemon 启动失败导致脚本直接退出。
    "$adb_path" kill-server >/dev/null 2>&1 || true
    "$adb_path" start-server >/dev/null 2>&1 || true
    adb_output="$("$adb_path" devices -l 2>&1 || true)"
  fi

  if echo "$adb_output" | grep -qE "$server_error_regex"; then
    echo "adb 无法启动或连接到 server（请先手动修复 adb 环境后再运行脚本）：" >&2
    echo "$adb_output" >&2
    echo "" >&2
    echo "可尝试（在你的终端里）：adb kill-server && adb start-server" >&2
    exit 1
  fi

  local device_serials=()
  local unauthorized_serials=()
  local offline_serials=()
  local other_states=()

  while read -r serial state _rest; do
    [[ -n "${serial:-}" ]] || continue
    [[ "$serial" == "List" ]] && continue
    [[ "$serial" == "*" ]] && continue
    [[ -n "${state:-}" ]] || continue

    case "$state" in
      device) device_serials+=("$serial") ;;
      unauthorized) unauthorized_serials+=("$serial") ;;
      offline) offline_serials+=("$serial") ;;
      *) other_states+=("$serial:$state") ;;
    esac
  done <<<"$adb_output"

  if [[ ${#device_serials[@]} -eq 1 ]]; then
    echo "${device_serials[0]}"
    return 0
  fi

  if [[ ${#device_serials[@]} -gt 1 ]]; then
    echo "Multiple devices detected; specify one with --serial or ANDROID_SERIAL:" >&2
    printf '  %s\n' "${device_serials[@]}" >&2
    exit 1
  fi

  if [[ ${#unauthorized_serials[@]} -gt 0 ]]; then
    echo "设备未授权（unauthorized）。请在真机上点“允许 USB 调试”，然后重试：" >&2
    printf '  %s\n' "${unauthorized_serials[@]}" >&2
    exit 1
  fi

  if [[ ${#offline_serials[@]} -gt 0 ]]; then
    echo "设备处于 offline 状态。请重新插拔数据线/重启 adb server 后重试：" >&2
    printf '  %s\n' "${offline_serials[@]}" >&2
    exit 1
  fi

  if [[ ${#other_states[@]} -gt 0 ]]; then
    echo "未找到可用 device 状态设备，当前设备状态：" >&2
    printf '  %s\n' "${other_states[@]}" >&2
    exit 1
  fi

  echo "No connected devices found. Check USB + 开发者选项(USB 调试) + 授权弹窗。" >&2
  exit 1
}

detect_app_id() {
  if [[ -n "$APP_ID" ]]; then
    echo "$APP_ID"
    return 0
  fi
  require_file "$APP_GRADLE"
  local id
  id="$(awk -F'"' '/applicationId[[:space:]]*=[[:space:]]*"/{print $2; exit}' "$APP_GRADLE" || true)"
  if [[ -z "$id" ]]; then
    echo "Unable to detect applicationId from $APP_GRADLE; pass --app-id." >&2
    exit 1
  fi
  echo "$id"
}

detect_launcher_activity() {
  if [[ -n "$ACTIVITY" ]]; then
    echo "$ACTIVITY"
    return 0
  fi
  require_file "$MANIFEST"

  # 简单解析：找到同一个 <activity> / <activity-alias> 块内同时包含 MAIN + LAUNCHER 的条目。
  local name
  name="$(
    awk '
      function reset() { in_block=0; has_main=0; has_launcher=0; block_name=""; }
      BEGIN { reset(); }
      /<activity[^>]*|<activity-alias[^>]*/ {
        reset();
        in_block=1;
        if (match($0, /android:name="[^"]+"/)) {
          block_name=substr($0, RSTART+length("android:name=\""), RLENGTH-length("android:name=\"")-1);
        }
      }
      in_block && /android:name="/ && block_name=="" {
        if (match($0, /android:name="[^"]+"/)) {
          block_name=substr($0, RSTART+length("android:name=\""), RLENGTH-length("android:name=\"")-1);
        }
      }
      in_block && /android\.intent\.action\.MAIN/ { has_main=1; }
      in_block && /android\.intent\.category\.LAUNCHER/ { has_launcher=1; }
      in_block && (/<\/activity>|<\/activity-alias>/) {
        if (has_main && has_launcher && block_name!="") { print block_name; exit; }
        reset();
      }
    ' "$MANIFEST"
  )"
  if [[ -z "$name" ]]; then
    echo "Unable to detect LAUNCHER activity from $MANIFEST; pass --activity (e.g. .MainActivity)." >&2
    exit 1
  fi
  echo "$name"
}

ADB_PATH="$(detect_adb)"
DEVICE_SERIAL="$(pick_serial "$ADB_PATH")"
APP_ID="$(detect_app_id)"
ACTIVITY="$(detect_launcher_activity)"

echo "Device: $DEVICE_SERIAL"
echo "AppId:  $APP_ID"
echo "Entry:  $ACTIVITY"

ensure_java17() {
  # Gradle 需要 JDK 17+。如果本机配置了较低版本（例如 JAVA_HOME=JDK11），这里强制切到 17。
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local java17_home
    java17_home="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [[ -n "$java17_home" ]]; then
      if [[ -n "${JAVA_HOME:-}" ]] && [[ "$JAVA_HOME" != "$java17_home" ]]; then
        echo "JAVA_HOME 当前不是 JDK17，已切换为：$java17_home" >&2
      fi
      export JAVA_HOME="$java17_home"
      return 0
    fi
  fi

  if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "未找到 JDK17。请安装 JDK 17，并设置 JAVA_HOME 后重试。" >&2
    exit 1
  fi
}

if [[ $DO_INSTALL -eq 1 ]]; then
  ensure_java17
  (cd "$PROJECT_ROOT" && ./gradlew :app:installDebug)
fi

"$ADB_PATH" -s "$DEVICE_SERIAL" shell am start -n "$APP_ID/$ACTIVITY" >/dev/null
echo "Launched: $APP_ID/$ACTIVITY"
