---
module: [kind=guide] startup_files
---

# Running code when a computer turns on

:::note Use case
You might be aware that CC computers restart when the chunk they are in reloads,  startup files provides a way to have the CC computer run arbitrary code after it has finished turning on.
:::

CC computers will look for a file or folder with a special name when it finishes loading. Also, CC has a fixed order in which it looks for these files and uses them.

## The magic names
All startup *files* have to be at the root of their drive, this drive is commonly the computer's internal storage, but can be a disk too.
```
startup
startup.lua
```
`startup` may be a file or a folder, the behaviour of it changes depending on which it is but we'll cover that in a bit. `startup.lua` has to be a file. Note that these are all lower case, case matters sometimes so keep to the correct one - more info in a note on the @{fs} API page.


## Searching drives for startup files
If `shell.allow_disk_startup` is true (which it is by default), then the CC computer will look for disk drives that have at least one startup file, it only uses the first disk that fulfils these criteria. Disk drives are searched in the order they are found via `peripheral.getNames()`. It then runs the found startup files as described below.

If `shell.allow_startup` is true (which it is by default) and no disk startup file was used, then the computer will look for startup files in its internal storage. Valid files are run as described below.

### Running the startup files
If `startup` is a file then this file and *only* this file is run, even if a `startup.lua` exists on the same device.

If `startup` is not a file (either it's a folder or it doesn't exist) then `startup.lua` is run, assuming it exists. After running `startup.lua` (or not if it didn't exist), the CC computer then runs each file in the `startup` folder one after the other in the order that @{fs.list} returns them.

:::note Example
Let's say that I have a computer with a startup file on its internal storage, and is connected to six disk drives. Two of these drives have disks that have startup files, one has a disk with no files, one with files but none of them are startup files, one has a music disk (the kind that you can place in a jukebox), the remaining drive has no disk in it.

Both of the settings that affect startup are set to allow their respective startup modes.

I turn on my computer, it looks for disk drives. It so happens that the first one it finds is the empty one, since this drive has no disk it goes to the next drive it found. The second drive that it searches for startup files is the jukebox music disk, this one is also skipped as music disks don't have startup files. Third, it finds the disk drive containing the disk without files, it goes to the next drive. Forth is the disk with files but without startup files, it tries the next disk.

We are down to the two disks both with startup files, the computer finds one of them and searches it for startup files, it finds the files and runs them in the order above. When/if these startup files exit without shutting down the computer then the CC computer drops into the interactive prompt. The computer *didn't* look at the last disk drive and *didn't* use the startup files on its internal storage.
:::

:::note Mass configuration
Unless disabled in the server config, CC computers will look for startup files on disks in attached disk drives. This means that a freshly crafted computer can automatically load a program from that disk startup file.

Apply this to turtles with a nearby chest containing fuel and you can have a turtle factory that automatically programs the turtles after creating them.
:::

:::note Computers don't remember what they were doing when the chunk unloads
You may have heard of the term "persistence", Computercraft computers do not have persistence. Startup files are currently the closest that CC has to persistence.

If you don't know what persistence is, if CC computers were persistent then they would remember what they were doing when the chunk they are in was unloaded, and resume running the task they were doing when the chunk gets reloaded.

However, Computercraft doesn't have persistence and thus restart as if they were just turned on. Mentioning turning on, they will remember that they were running and when the chunk reloads they will turn themselves on and start looking for startup files. Clever use of startup files and recording data to disk can somewhat circumvent the lack of built in persistence.

For most programs (usually running on computers), restarting from the beginning of the program is not an issue. However, turtles commonly have startup complications due to their ability to move. Unless you make sure that the turtle is in a fixed starting position every time (which is not always practical), your turtle programs will need some way to cope with the turtle starting in a different position - perhaps with the @{gps} API.

Incase it's not clear, Minecraft unloads all chunks (including force loaded chunks with chunk loaders) when the server restarts and when the single player world closes. So you may want startup files even if you are chunk loading your computers.
:::
