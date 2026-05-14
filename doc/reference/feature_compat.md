---
module: [kind=reference] feature_compat
---

<!--
SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Lua 5.2/5.3 features in CC: Tweaked
CC: Tweaked is based off of the Cobalt Lua runtime, which uses Lua 5.2. However, Cobalt and CC:T implement additional
features from Lua 5.2 and 5.3 (as well as some deprecated 5.0 and 5.1 features). This page lists all of the
compatibility for these newer versions.

## Lua 5.2
| Feature                                                       | Supported? | Notes                                                             |
|---------------------------------------------------------------|------------|-------------------------------------------------------------------|
| `goto`/labels                                                 | ✔          |                                                                   |
| `_ENV`                                                        | ✔          |                                                                   |
| `\z` escape                                                   | ✔          |                                                                   |
| `\xNN` escape                                                 | ✔          |                                                                   |
| Hex literal fractional/exponent parts                         | ✔          |                                                                   |
| Empty statements                                              | ✔          |                                                                   |
| `__len` metamethod                                            | ✔          |                                                                   |
| `__ipairs` metamethod                                         | ❌         | Deprecated in Lua 5.3. `ipairs` uses `__len`/`__index` instead.   |
| `__pairs` metamethod                                          | ✔          |                                                                   |
| `bit32` library                                               | ✔          |                                                                   |
| `collectgarbage` isrunning, generational, incremental options | ❌         | `collectgarbage` does not exist in CC:T.                          |
| New `load` syntax                                             | ✔          |                                                                   |
| `loadfile` mode parameter                                     | ✔          | Supports both 5.1 and 5.2+ syntax.                                |
| Removed `loadstring`                                          | ❌         |                                                                   |
| Removed `getfenv`, `setfenv`                                  | 🔶         | Only supports closures with an `_ENV` upvalue.                    |
| `rawlen` function                                             | ✔          |                                                                   |
| Negative index to `select`                                    | ✔          |                                                                   |
| Removed `unpack`                                              | ❌         |                                                                   |
| Arguments to `xpcall`                                         | ✔          |                                                                   |
| Second return value from `coroutine.running`                  | ✔          |                                                                   |
| Removed `module`                                              | ✔          |                                                                   |
| `package.loaders` -> `package.searchers`                      | ❌         |                                                                   |
| Second argument to loader functions                           | ✔          |                                                                   |
| `package.config`                                              | ✔          |                                                                   |
| `package.searchpath`                                          | ✔          |                                                                   |
| Removed `package.seeall`                                      | ✔          |                                                                   |
| `string.dump` on functions with upvalues (blanks them out)    | ❌         | `string.dump` is not supported                                    |
| `string.rep` separator                                        | ✔          |                                                                   |
| `%g` match group                                              | ❌         |                                                                   |
| Removal of `%z` match group                                   | ❌         |                                                                   |
| Removed `table.maxn`                                          | ❌         |                                                                   |
| `table.pack`/`table.unpack`                                   | ✔          |                                                                   |
| `math.log` base argument                                      | ✔          |                                                                   |
| Removed `math.log10`                                          | ❌         |                                                                   |
| `*L` mode to `file:read`                                      | ✔          |                                                                   |
| `os.execute` exit type + return value                         | ❌         | `os.execute` does not exist in CC:T.                              |
| `os.exit` close argument                                      | ❌         | `os.exit` does not exist in CC:T.                                 |
| `istailcall` field in `debug.getinfo`                         | ❌         |                                                                   |
| `nparams` field in `debug.getinfo`                            | ✔          |                                                                   |
| `isvararg` field in `debug.getinfo`                           | ✔          |                                                                   |
| `debug.getlocal` negative indices for varargs                 | ❌         |                                                                   |
| `debug.getuservalue`/`debug.setuservalue`                     | ❌         | Userdata are rarely used in CC:T, so this is not necessary.       |
| `debug.upvalueid`                                             | ✔          |                                                                   |
| `debug.upvaluejoin`                                           | ✔          |                                                                   |
| Tail call hooks                                               | ❌         |                                                                   |
| `=` prefix for chunks                                         | ✔          |                                                                   |
| Yield across C boundary                                       | ✔          |                                                                   |
| Removal of ambiguity error                                    | ✔          |                                                                   |
| Identifiers may no longer use locale-dependent letters        | ✔          |                                                                   |
| Ephemeron tables                                              | ❌         |                                                                   |
| Identical functions may be reused                             | ❌         | Removed in Lua 5.4                                                |
| Generational garbage collector                                | ❌         | Cobalt uses the built-in Java garbage collector.                  |

## Lua 5.3
| Feature                                                                               | Supported? | Notes                     |
|---------------------------------------------------------------------------------------|------------|---------------------------|
| Integer subtype                                                                       | ❌         |                           |
| Bitwise operators/floor division                                                      | ❌         |                           |
| `\u{XXX}` escape sequence                                                             | ✔          |                           |
| `utf8` library                                                                        | ✔          |                           |
| removed `__ipairs` metamethod                                                         | ✔          |                           |
| `coroutine.isyieldable`                                                               | ✔          |                           |
| `string.dump` strip argument                                                          | ✔          |                           |
| `string.pack`/`string.unpack`/`string.packsize`                                       | ✔          |                           |
| `table.move`                                                                          | ✔          |                           |
| `math.atan2` -> `math.atan`                                                           | 🔶         | `math.atan` supports its two argument form. |
| Removed `math.frexp`, `math.ldexp`, `math.pow`, `math.cosh`, `math.sinh`, `math.tanh` | ❌         |                           |
| `math.maxinteger`/`math.mininteger`                                                   | ❌         |                           |
| `math.tointeger`                                                                      | ❌         |                           |
| `math.type`                                                                           | ❌         |                           |
| `math.ult`                                                                            | ❌         |                           |
| Removed `bit32` library                                                               | ❌         |                           |
| Remove `*` from `file:read` modes                                                     | ✔          |                           |
| Metamethods respected in `table.*`, `ipairs`                                          | ✔          |                           |

## Lua 5.5
| Feature                            | Supported? | Notes |
|------------------------------------|------------|-------|
| `table.create`                     | ✔          |       |
| `utf8.offset` returns end position | ✔          |       |

## Lua 5.0
| Feature                          | Supported? | Notes                                            |
|----------------------------------|------------|--------------------------------------------------|
| `arg` table                      | 🔶         | Only set in the shell - not used in functions.   |
| `string.gfind`                   | ✔          | Equal to `string.gmatch`.                        |
| `table.getn`                     | ✔          | Equal to `#tbl`.                                 |
| `table.setn`                     | ❌         |                                                  |
| `math.mod`                       | ✔          | Equal to `math.fmod`.                            |
| `table.foreach`/`table.foreachi` | ✔          |                                                  |
| `gcinfo`                         | ❌         | Cobalt uses the built-in Java garbage collector. |
