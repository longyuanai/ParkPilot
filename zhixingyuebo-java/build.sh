#!/usr/bin/env bash
# ====================================================================
# ParkPilot (智泊领航) - Linux / macOS POSIX Build Script
# ====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${SCRIPT_DIR}/src"
LIB_DIR="${SCRIPT_DIR}/lib"
RES_DIR="${SCRIPT_DIR}/resources"
BIN_DIR="${SCRIPT_DIR}/bin"

echo "=== [ParkPilot] Starting clean build on Linux / POSIX ==="
rm -rf "${BIN_DIR}"
mkdir -p "${BIN_DIR}"

# Find all Java files
JAVA_FILES=$(find "${SRC_DIR}" -name "*.java")

# Compile with Java 8 bytecode compatibility
javac -encoding UTF-8 -source 8 -target 8 \
      -cp "${LIB_DIR}/mysql-connector-j-8.1.0.jar" \
      -d "${BIN_DIR}" \
      ${JAVA_FILES}

# Copy classpath resources
if [ -f "${RES_DIR}/db.local.properties" ]; then
    cp "${RES_DIR}/db.local.properties" "${BIN_DIR}/"
fi
cp "${RES_DIR}/db.properties" "${BIN_DIR}/"

FILE_COUNT=$(echo "${JAVA_FILES}" | wc -w)
echo "=== [ParkPilot] BUILD PASS: ${FILE_COUNT} Java files compiled to ${BIN_DIR} ==="
