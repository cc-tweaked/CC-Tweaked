New features in CC: Tweaked 1.97.0

* Update several translations (Anavrins, Jummit, Naheulf).
* Add button to view a computer's folder to `/computercraft dump`.
* Allow cleaning dyed turtles in a cauldron.
* Add scale subcommand to `monitor` program (MCJack123).
* Add option to make `textutils.serialize` not write an indent (magiczocker10).
* Allow comparing vectors using `==` (fatboychummy).
* Improve HTTP error messages for SSL failures.
* Allow `craft` program to craft unlimited items (fatboychummy).
* Impose some limits on various command queues.
* Add buttons to shutdown and terminate to computer GUIs.
* Add program subcompletion to several programs (Wojbie).
* Update the `help` program to accept and (partially) highlight markdown files.
* Remove config option for the debug API.
* Allow setting the subprotocol header for websockets.

And several bug fixes:
* Fix NPE when using a treasure disk when no treasure disks are available.
* Prevent command computers discarding command ouput when certain game rules are off.
* Fix turtles not updating peripherals when upgrades are unequipped (Ronan-H).
* Fix computers not shutting down on fatal errors within the Lua VM.
* Speakers now correctly stop playing when broken, and sound follows noisy turtles and pocket computers.
* Update the `wget` to be more resiliant in the face of user-errors.
* Fix exiting `paint` typing "e" in the shell.

Type "help changelog" to see the full version history.
