New features in CC: Tweaked 1.101.0

* Improve Dutch translation (Quezler)
* Better reporting of fatal computer timeouts in the server log.
* Convert detail providers into a registry, allowing peripheral mods to read item/block details.
* Redesign the metrics system. `/computercraft track` now allows computing aggregates (total, max, avg) on any metric, not just computer time.
* File drag-and-drop now queues a `file_transfer` event on the computer. The
  built-in shell or the `import` program must now be running to upload files.
* The `peripheral` now searches for remote peripherals using any peripheral with the `peripheral_hub` type, not just wired modems.
* Add `include_hidden` option to `fs.complete`, which can be used to prevent hidden files showing up in autocomplete results. (IvoLeal72)
* Add `shell.autocomplete_hidden` setting. (IvoLeal72)

Several bug fixes:
* Prevent `edit`'s "Run" command scrolling the terminal output on smaller
  screens.
* Remove some non-determinism in computing item's `nbt` hash.
* Don't set the `Origin` header on outgoing websocket requests.

Type "help changelog" to see the full version history.
