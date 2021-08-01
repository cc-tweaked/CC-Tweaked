New features in CC: Tweaked 1.98.0
* Add motd for file uploading.
* Add config options to limit total bandwidth used by the HTTP API.

And several bug fixes:
* Fix `settings.define` not accepting a nil second argument (SkyTheCodeMaster).
* Various documentation fixes (Angalexik, emiliskiskis, SkyTheCodeMaster).
* Fix selected slot indicator not appearing in turtle interface.
* Fix crash when printers are placed as part of world generation.
* Fix crash when breaking a speaker on a multiplayer world.
* Add a missing type check for `http.checkURL`.
* Prevent `parallel.*` from hanging when no arguments are given.
* Prevent issue in rednet when the message ID is NaN.
* Fix `help` program crashing when terminal changes width.
* Ensure monitors are well-formed when placed, preventing graphical glitches
  when using Carry On or Quark.
* Accept several more extensions in the websocket client.
* Prevent `wget` crashing when given an invalid URL and no filename.
* Correctly wrap string within `textutils.slowWrite`.

Type "help changelog" to see the full version history.
