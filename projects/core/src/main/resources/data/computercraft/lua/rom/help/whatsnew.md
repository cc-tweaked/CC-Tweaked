New features in CC: Tweaked 1.119.0

* Add `commands.getDimension()`.
* Add `cc.base64` module.
* Update Cobalt to 0.9.9, bringing in several Lua 5.5 changes:
  * Floats are now printed with enough digits to round trip correctly.
  * Add `table.create`.
  * `utf8.offset` now returns the final position of the codepoint.

Several bug fixes:
* Fix handling of integer indexes in `LuaTable`.
* Correct `min` and `sec` defaults in `os.time`. (sircfenner)
* Make HTTP IP filtering stricter.

Type "help changelog" to see the full version history.
