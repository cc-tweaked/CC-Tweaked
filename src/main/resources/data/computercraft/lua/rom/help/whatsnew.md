New features in CC: Tweaked 1.100.6

* Various documentation improvements (MCJack123, FayneAldan).
* Allow CC's blocks to be rotated when used in structure blocks (Seniorendi).
* Several performance improvements to computer execution.
* Add parse_empty_array option to textutils.unserialiseJSON (@ChickChicky).
* Add an API to allow other mods to provide extra item/block details (Lemmmy).
* All blocks with GUIs can now be "locked" (via a command or NBT editing tools) like vanilla inventories. Players can only interact with them with a specific named item.

Several bug fixes:
* Fix printouts being rendered with an offset in item frames (coolsa).
* Reduce position latency when playing audio with a noisy pocket computer.
* Fix total counts in /computercraft turn-on/shutdown commands.
* Fix "Run" command not working in the editor when run from a subdirectory (Wojbie).
* Pocket computers correctly preserve their on state.

Type "help changelog" to see the full version history.
