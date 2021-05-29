---
module: [kind=event] modem_message
---

The @{modem_message} event is fired when a message is received on an open channel on any modem.

## Return Values
1. @{string}: The event name.
2. @{string}: The side of the modem that received the message.
3. @{number}: The channel that the message was sent on.
4. @{number}: The reply channel set by the sender.
5. @{any}: The message as sent by the sender.
6. @{number}: The distance between the sender and the receiver, in blocks (decimal).

## Example
Prints a message when one is sent:
```lua
while true do
  local event, side, channel, replyChannel, message, distance = os.pullEvent("modem_message")
  print(("Message received on side %s on channel %d (reply to %d) from %f blocks away with message %s"):format(side, channel, replyChannel, distance, tostring(message)))
end
```
