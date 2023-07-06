New features in CC: Tweaked 1.101.3

* Improve syntax errors when missing commas in tables, and on trailing commas in parameter lists.
* `speaker` program now reports an error on common unsupported audio formats.
* Small optimisations to the `window` API.

Several bug fixes:
* Fix the REPL syntax reporting crashing on valid parses.
* Ignore metatables in `textutils.serialize`.
* Fix `gps.locate` returning `nan` when receiving a duplicate location (Wojbie).
* Ignore metatables in `textutils.serialize`.
* Fix wireless turtles having an invalid model.
* Fix crash when turtles are exploded by a null explosion.
* Lua REPL no longer accepts `)(` as a valid expression.
* Fix several inconsistencies with `require`/`package.path` in the Lua REPL (Wojbie).
* Fix private several IP address ranges not being blocked by the `$private` rule.
* Improve permission checks in the `/computercraft` command.

Type "help changelog" to see the full version history.
