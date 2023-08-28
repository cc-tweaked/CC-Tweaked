New features in CC: Tweaked 1.108.0

* Remove compression from terminal/monitor packets. Vanilla applies its own compression, so this ends up being less helpful than expected.
* `/computercraft` command now supports permission mods.
* Split some GUI textures into sprite sheets.
* Support the `%g` character class in string pattern matching.

Several bug fixes:
* Fix crash when playing some modded records via a disk drive.
* Fix race condition when computers attach or detach from a monitor.
* Fix the "max websocket message" config option not being read.
* `tostring` now correctly obeys `__name`.
* Fix several inconsistencies with pattern matching character classes.

Type "help changelog" to see the full version history.
