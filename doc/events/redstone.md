---
module: [kind=event] redstone
---

The @{event!redstone} event is fired whenever any redstone inputs on the computer change.

## Example
Prints a message when a redstone input changes:
```lua
while true do
  os.pullEvent("redstone")
  print("A redstone input has changed!")
end
```
