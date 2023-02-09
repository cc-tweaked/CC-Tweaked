New features in CC: Tweaked 1.103.0

* The shell now supports hashbangs (`#!`) (emmachase).
* Error messages in `edit` are now displayed in red on advanced computers.
* `turtle.getItemDetail` now always includes the `nbt` hash.
* Improvements to the display of errors in the shell and REPL.
* Turtles, pocket computers, and disks can be undyed by careful application (i.e. crafting) of a sponge.
* Turtles can no longer be dyed/undyed by right clicking.

Several bug fixes:
* Several documentation improvements and fixes (ouroborus, LelouBil)
* Fix rednet queueing the wrong message when sending a message to the current computer.
* Fix the Lua VM crashing when a `__len` metamethod yields.
* `pocket.{un,}equipBack` now correctly copies the stack when unequipping an upgrade.
* Fix `key` events not being queued while pressing computer shortcuts.

Type "help changelog" to see the full version history.
