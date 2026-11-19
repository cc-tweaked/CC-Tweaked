New features in CC: Tweaked 1.122.0

* Improve support in the API for peripherals targetting blocks.
* Wired modems now only activate when clicking the modem part.

Several bug fixes:
* Fix pocket computers "swinging" in the hand when the item changes.
* Fix several crashes introduced by 1.121.0's upgrade refactor.
* Fix `shell.openTab` dropping arguments.
* Fix computers turning on again after shutting down if the GUI is opened twice in quick succession.
* Update Cobalt to 0.9.10:
  * Make divide-by-zero behaviour more consistent with PUC Lua.
  * Fix validation of userdata metamethods in the `table` library.

Type "help changelog" to see the full version history.
