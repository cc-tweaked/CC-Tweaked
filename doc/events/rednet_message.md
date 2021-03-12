---
module: [kind=event] rednet_message
see: modem_message For raw modem messages sent outside of Rednet.
see: rednet.receive To wait for a Rednet message with an optional timeout and protocol filter.
---

The @{rednet_message} event is fired when a message is sent over Rednet.

This event is usually handled by @{rednet.receive}, but it can also be pulled manually.

@{rednet_message} events are sent by @{rednet.run} in the top-level coroutine in response to @{modem_message} events. A @{rednet_message} event is always preceded by a @{modem_message} event. They are generated inside CraftOS rather than being sent by the ComputerCraft machine.

## Return Values
1. @{string}: The event name.
2. @{number}: The ID of the sending computer.
3. @{any}: The message sent.
4. @{string|nil}: The protocol of the message, if provided.

## Example
Prints a message when one is sent:
```lua
while true do
  local event, sender, message, protocol = os.pullEvent("rednet_message")
  if protocol ~= nil then
    print("Received message from " .. sender .. " with protocol " .. protocol .. " and message " .. tostring(message))
  else
    print("Received message from " .. sender .. " with message " .. tostring(message))
  end
end
```
