---
module: [kind=event] monitor_resize
---

The @{monitor_resize} event is fired when an adjacent or networked monitor's size is changed.

## Return Values
1. @{string}: The event name.
2. @{string}: The side or network ID of the monitor that resized.

## Example
Prints a message when a monitor is resized:
```lua
while true do
  local event, side = os.pullEvent("monitor_resize")
  print("The monitor on side " .. side .. " was resized.")
end
```
