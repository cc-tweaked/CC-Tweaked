# LuaJIT 替换说明

## 文件改动（6个）

| 文件 | 改动 |
|---|---|
| `gradle/libs.versions.toml` | +JNA 5.14.0 |
| `projects/core/build.gradle.kts` | +`implementation(libs.jna)` |
| `ComputerContext.java` | 默认工厂 `CobaltLuaMachine::new` → `LuaJITMachine::new` |
| `luajit/LuaJITLib.java` | **新增** JNA 绑定（~150行，含自解压加载器） |
| `luajit/LuaJITMachine.java` | **新增** ILuaMachine 实现（~440行） |
| `rom/lua53_shim.lua` | **新增** 5.3兼容垫片（utf8, table.move 等） |
| `.github/workflows/build-luajit-native.yml` | **新增** CI workflow 编译 native 库 |

---

## 获取 native 库（两步）

LuaJIT 是 C 代码，需要编译为 `.so`（Linux）和 `.dll`（Windows）。  
你本地内存不够编译，所以用 **CI 来编译**：

### 第一步：触发 CI

把这个仓库 push 到 GitHub，或者手动触发 workflow：

```
GitHub → Actions → "Build LuaJIT native libraries" → Run workflow
```

CI 会做两件事（并行）：
- **Ubuntu 上** 用 `gcc` 编译 `libluajit.so`
- **Ubuntu 上** 用 `x86_64-w64-mingw32-gcc` 交叉编译 `luajit.dll`（不需要 Windows 机器）

### 第二步：下载产物放到正确位置

CI 完成后，从 Actions 页面下载两个 artifact：

| Artifact | 放到 |
|---|---|
| `luajit-linux-x86-64` → `libluajit.so` | `projects/core/src/main/resources/native/linux-x86-64/libluajit.so` |
| `luajit-windows-x86-64` → `luajit.dll` | `projects/core/src/main/resources/native/windows-x86-64/luajit.dll` |

目录结构：
```
projects/core/src/main/resources/native/
  linux-x86-64/
    libluajit.so     (≈ 400 KB)
  windows-x86-64/
    luajit.dll       (≈ 600 KB)
```

### 第三步：打包

放好之后，正常 `./gradlew build` 就会把 native 库打进 mod jar。  
运行时 `LuaJITLib.java` 自动解压到临时目录加载，不需要安装任何东西。

---

## 加载流程

```
LuaJITLib.INSTANCE（首次访问）
  │
  ▼
NativeLuajitLoader.load()
  ├─ 从 jar 中提取 /native/linux-x86-64/libluajit.so
  │    到临时目录 ${TMP}/cc-luajit-xxxx/libluajit.so
  │    (deleteOnExit 自动清理)
  │
  ▼
JNA Native.load(path, LuaJITLib.class)
  │  → dlopen(path) → libluajit.so
  │  → JNA 映射 Java 方法到 C 函数指针
  │
  ▼ 就绪
luaL_newstate() / lua_resume() / lua_close()
```

---

## 已知限制

- **中断超时**：`lua_yield` / `lua_error` 在 JNA 回调中用 longjmp 实现。  
  对个人 mod 足够安全，风险是如果回调持有 Java 锁会 leak——目前所有回调都无锁。
- **协程**：LuaJIT 是 5.1 语义（不能在 pcall 外用 lua_yield），CC:T 的 `os.pullEvent` 本身就是顶层调用，无冲突。
- **5.3 垫片**：覆盖 `utf8.*`、`math.tointeger/type`、`table.move`。  
  `string.pack/unpack` 暂未实现，飞控如果用到了要补。
