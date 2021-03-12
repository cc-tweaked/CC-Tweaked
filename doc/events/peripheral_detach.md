---
module: [kind=event] peripheral_detach
see: peripheral For the event fired when a peripheral is attached.
---

The @{peripheral_detach} event is fired when a peripheral is detached from a side or from a modem.

## Return Values
1. @{string}: The event name.
2. @{string}: The side the peripheral was detached from.

## Example
Prints a message when a peripheral is detached:
```lua
while true do
  local event, side = os.pullEvent("peripheral_detach")
  print("A peripheral was detached on side " .. side)
end
```
