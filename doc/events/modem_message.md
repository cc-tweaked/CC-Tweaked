---
module: [kind=event] modem_message
---

The @{modem_message} event is fired when a message is received on an open channel on any modem.

## Return Values
1. @{string}: The side of the modem that received the message.
2. @{number}: The channel that the message was sent on.
3. @{number}: The reply channel set by the sender.
4. @{any}: The message as sent by the sender.
5. @{number}: The distance between the sender and the receiver, in blocks (decimal).

## Example
Prints a message when one is sent:
```lua
while true do
  local event, side, channel, replyChannel, message, distance = os.pullEvent("modem_message")
  print("Message received on side " .. side .. " on channel " .. channel .. " (reply to " .. replyChannel .. ") from " .. distance .. " blocks away with message " .. tostring(message))
end
```
