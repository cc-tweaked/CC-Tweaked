New features in CC: Tweaked 1.102.0

* `fs.isReadOnly` now reads filesystem attributes (Lemmmy).
* `IComputerAccess.executeMainThreadTask` no longer roundtrips values through Lua.
* The turtle label now animates when the turtle moves.

Several bug fixes:
* Trim spaces from filesystem paths.
* Correctly format 12AM/PM with `%I`.
* Fix `import.lua` failing to upload a file.
* Fix duplicated swing animations on high-ping servers (emmachase).
* Fix several issues with sparse Lua tables (Shiranuit).

Type "help changelog" to see the full version history.
