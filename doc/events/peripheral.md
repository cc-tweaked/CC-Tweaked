---
module: [kind=event] peripheral
see: peripheral_detach For the event fired when a peripheral is detached.
---

The @{peripheral} event is fired when a peripheral is attached on a side or to a modem.

## Return Values
1. @{string}: The event name.
2. @{string}: The side the peripheral was attached to.

## Example
Prints a message when a peripheral is attached:
```lua
while true do
  local event, side = os.pullEvent("peripheral")
  print("A peripheral was attached on side " .. side)
end
```
