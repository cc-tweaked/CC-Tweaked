---
module: [kind=event] disk
see: disk_eject For the event sent when a disk is removed.
---

The @{disk} event is fired when a disk is inserted into an adjacent or networked disk drive.

## Return Values
1. @{string}: The event name.
2. @{string}: The side of the disk drive that had a disk inserted.

## Example
Prints a message when a disk is inserted:
```lua
while true do
  local event, side = os.pullEvent("disk")
  print("Inserted a disk on side " .. side)
end
```
