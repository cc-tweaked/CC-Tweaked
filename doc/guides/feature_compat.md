---
module: [kind=guide] feature_compat
---

# Lua 5.2/5.3 features in CC: Tweaked
CC: Tweaked is based off of the Cobalt Lua runtime, which uses Lua 5.1. However, Cobalt and CC:T implement additional features from Lua 5.2 and 5.3 (as well as some deprecated 5.0 features) that are not available in base 5.1. This page lists all of the compatibility for these newer versions.

## Lua 5.2
| Feature | Supported? | Notes |
|---------|------------|-------|
| `goto`/labels | :x: |  |
| `_ENV` | :large_orange_diamond: | The `_ENV` global points to `getfenv()`, but it cannot be set. |
| `\z` escape | :heavy_check_mark: |  |
| `\xNN` escape | :heavy_check_mark: |  |
| hex literal fractional/exponent parts | :heavy_check_mark: |  |
| empty statements | :x: |  |
| `__len` metamethod | :heavy_check_mark: |  |
| `__ipairs` metamethod | :x: |  |
| `__pairs` metamethod | :heavy_check_mark: |  |
| `bit32` library | :heavy_check_mark: | Replaces `bit` library, which is still available for compatibility. |
| `collectgarbage` isrunning, generational, incremental options | :x: | `collectgarbage` does not exist in CC:T. |
| new `load` syntax | :heavy_check_mark: |  |
| `loadfile` mode parameter | :heavy_check_mark: | Supports both 5.1 and 5.2+ syntax. |
| removed `loadstring` | :large_orange_diamond: | Only if `disable_lua51_features` is enabled in the configuration. |
| removed `getfenv`, `setfenv` | :large_orange_diamond: | Only if `disable_lua51_features` is enabled in the configuration. |
| `rawlen` function | :heavy_check_mark: |  |
| negative index to `select` | :heavy_check_mark: |  |
| removed `unpack` | :large_orange_diamond: | Only if `disable_lua51_features` is enabled in the configuration. |
| arguments to `xpcall` | :x: |  |
| second return value from `coroutine.running` | :x: |  |
| removed `module` | :heavy_check_mark: |  |
| `package.loaders` -> `package.searchers` | :x: |  |
| second argument to loader functions | :heavy_check_mark: |  |
| `package.config` | :heavy_check_mark: |  |
| `package.searchpath` | :heavy_check_mark: |  |
| removed `package.seeall` | :heavy_check_mark: |  |
| `string.dump` on functions with upvalues (blanks them out) | :heavy_check_mark: |  |
| `string.rep` separator | :x: |  |
| `%g` match group | :x: |  |
| removal of `%z` match group | :x: |  |
| removed `table.maxn` | :large_orange_diamond: | Only if `disable_lua51_features` is enabled in the configuration. |
| `table.pack`/`table.unpack` | :heavy_check_mark: |  |
| `math.log` base argument | :heavy_check_mark: |  |
| removed `math.log10` | :large_orange_diamond: | Only if `disable_lua51_features` is enabled in the configuration. |
| `*L` mode to `file:read` | :heavy_check_mark: |  |
| `os.execute` exit type + return value | :x: | `os.execute` does not exist in CC:T. |
| `os.exit` close argument | :x: | `os.exit` does not exist in CC:T. |
| `istailcall` field in `debug.getinfo` | :x: |  |
| `nparams` field in `debug.getinfo` | :heavy_check_mark: |  |
| `isvararg` field in `debug.getinfo` | :heavy_check_mark: |  |
| `debug.getlocal` negative indices for varargs | :x: |  |
| `debug.getuservalue`/`debug.setuservalue` | :x: | Userdata are rarely used in CC:T, so this is not necessary. |
| `debug.upvalueid` | :heavy_check_mark: |  |
| `debug.upvaluejoin` | :heavy_check_mark: |  |
| tail call hooks | :x: |  |
| `=` prefix for chunks | :heavy_check_mark: |  |
| yield across C boundary | :heavy_check_mark: |  |
| removal of ambiguity error | :x: |  |
| identifiers may no longer use locale-dependent letters | :heavy_check_mark: |  |
| ephemeron tables | :x: | Weak tables are not supported. |
| functions may be reused | :x: |  |
| generational garbage collector | :x: | Cobalt uses the built-in Java garbage collector. |

## Lua 5.3
| Feature | Supported? | Notes |
|---------|------------|-------|
| integer subtype | :x: |  |
| bitwise operators/floor division | :x: |  |
| `\u{XXX}` escape sequence | :heavy_check_mark: |  |
| `utf8` library | :heavy_check_mark: |  |
| removed `__ipairs` metamethod | :heavy_check_mark: |  |
| `coroutine.isyieldable` | :x: |  |
| `string.dump` strip argument | :heavy_check_mark: |  |
| `string.pack`/`string.unpack`/`string.packsize` | :heavy_check_mark: |  |
| `table.move` | :x: |  |
| `math.atan2` -> `math.atan` | :x: |  |
| removed `math.frexp`, `math.ldexp`, `math.pow`, `math.cosh`, `math.sinh`, `math.tanh` | :x: |  |
| `math.maxinteger`/`math.mininteger` | :x: |  |
| `math.tointeger` | :x: |  |
| `math.type` | :x: |  |
| `math.ult` | :x: |  |
| removed `bit32` library | :x: |  |
| remove `*` from `file:read` modes | :heavy_check_mark: |  |
| metamethods respected in `table.*`, `ipairs` | :heavy_check_mark: |  |

## Lua 5.0
| Feature | Supported? | Notes |
|---------|------------|-------|
| `arg` table | :heavy_check_mark: | Only set in the shell - not used in functions. |
| `string.gfind` | :heavy_check_mark: | Equal to `string.gmatch`. |
| `table.getn` | :heavy_check_mark: | Equal to `#tbl`. |
| `table.setn` | :x: |  |
| `math.mod` | :heavy_check_mark: | Equal to `math.fmod`. |
| `table.foreach`/`table.foreachi` | :heavy_check_mark: |  |
| `gcinfo` | :x: | Cobalt uses the built-in Java garbage collector. |
