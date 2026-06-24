#!/usr/bin/env bash
# Build LuaJIT for Linux x64 and Windows x64, placing the binaries
# into the correct jar resource paths for embedded loading.
#
# Prerequisites:
#   Linux:  gcc, make
#   Windows: x86_64-w64-mingw32-gcc (apt install gcc-mingw-w64)
#
# Usage:  ./build-luajit-native.sh
#
# Output:
#   src/main/resources/native/linux-x86-64/libluajit.so
#   src/main/resources/native/windows-x86-64/luajit.dll

set -euo pipefail

LUAJIT_VERSION="v2.1"
LUAJIT_DIR="luajit-build"
OUTPUT_DIR="src/main/resources/native"

# ── Clone LuaJIT if not present ────────────────────────────────
if [ ! -d "$LUAJIT_DIR" ]; then
    echo "=== Cloning LuaJIT $LUAJIT_VERSION ==="
    git clone --depth 1 --branch "$LUAJIT_VERSION" \
        https://github.com/LuaJIT/LuaJIT.git "$LUAJIT_DIR"
fi

cd "$LUAJIT_DIR"

# ── Build for Linux x64 ────────────────────────────────────────
echo "=== Building Linux x64 ==="
make clean 2>/dev/null || true
make -j"$(nproc)" BUILDMODE=static CC=gcc \
    CFLAGS="-O2 -fPIC" \
    XCFLAGS="-DLUAJIT_ENABLE_LUA52COMPAT"

# Link as shared lib
gcc -shared -o libluajit.so \
    src/libluajit.a \
    -lm -ldl -lpthread
strip libluajit.so

OUT_LINUX="$OUTPUT_DIR/linux-x86-64"
mkdir -p "../$OUT_LINUX"
cp libluajit.so "../$OUT_LINUX/"
echo "  → $OUT_LINUX/libluajit.so ($(du -h libluajit.so | cut -f1))"

# ── Build for Windows x64 (cross-compile) ──────────────────────
echo "=== Building Windows x64 ==="
make clean 2>/dev/null || true
make -j"$(nproc)" \
    BUILDMODE=static \
    CC=x86_64-w64-mingw32-gcc \
    AR=x86_64-w64-mingw32-ar \
    CROSS=x86_64-w64-mingw32- \
    HOST_CC=gcc \
    TARGET_SYS=Windows \
    CFLAGS="-O2" \
    XCFLAGS="-DLUAJIT_ENABLE_LUA52COMPAT"

# Link as DLL
x86_64-w64-mingw32-gcc -shared -o luajit.dll \
    src/libluajit.a \
    -Wl,--out-implib=libluajit.dll.a
x86_64-w64-mingw32-strip luajit.dll

OUT_WIN="$OUTPUT_DIR/windows-x86-64"
mkdir -p "../$OUT_WIN"
cp luajit.dll "../$OUT_WIN/"
echo "  → $OUT_WIN/luajit.dll ($(du -h luajit.dll | cut -f1))"

echo "=== Done ==="
