#!/usr/bin/env bash
# ====================================================================
# ParkPilot (智泊领航) - Linux / macOS POSIX Run Script
# ====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN_DIR="${SCRIPT_DIR}/bin"
LIB_DIR="${SCRIPT_DIR}/lib"

if [ ! -d "${BIN_DIR}" ] || [ -z "$(ls -A "${BIN_DIR}" 2>/dev/null)" ]; then
    echo "=== [ParkPilot] Binaries not found, triggering build... ==="
    bash "${SCRIPT_DIR}/build.sh"
fi

CLASSPATH="${BIN_DIR}:${LIB_DIR}/mysql-connector-j-8.1.0.jar"

echo "=== [ParkPilot] Launching ParkPilot Console Application... ==="
java -Dfile.encoding=UTF-8 \
     -Duser.timezone=Asia/Shanghai \
     -cp "${CLASSPATH}" \
     com.zhixingyuebo.MainApp "$@"
