New features in CC: Tweaked 1.109.0

* Update to Lua 5.2
  * `getfenv`/`setfenv` now only work on Lua functions.
  * Add support for `goto`.
  * Remove support for dumping and loading binary chunks.
* File handles, HTTP requests and websocket messages now use raw bytes rather than converting to UTF-8.
* Add `allow_repetitions` option to `textutils.serialiseJSON`.
* Track memory allocated by computers.

Several bug fixes:
* Fix error when using position captures and backreferences in string patterns (e.g. `()(%1)`).
* Fix formatting non-real numbers with `%d`.

Type "help changelog" to see the full version history.
