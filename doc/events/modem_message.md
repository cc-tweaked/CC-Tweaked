---
module: [kind=event] modem_message
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`modem_message`] event is fired when a message is received on an open channel on any [`modem`].

## Return Values
1. [`string`]: The event name.
2. [`string`]: The side of the modem that received the message.
3. [`number`]: The channel that the message was sent on.
4. [`number`]: The reply channel set by the sender.
5. [`any`]: The message as sent by the sender.
6. <span class="type">[`number`]|[`nil`]</span>: The distance between the sender and the receiver in blocks, or [`nil`] if the message was sent between dimensions.

## Example
Wraps a [`modem`] peripheral, opens channel 0 for listening, and prints all received messages.

```lua
local modem = peripheral.find("modem") or error("No modem attached", 0)
modem.open(0)

while true do
    local event, side, channel, replyChannel, message, distance = os.pullEvent("modem_message")
    print(("Message received on side %s on channel %d (reply to %d) from %f blocks away with message %s"):format(
        side, channel, replyChannel, distance, tostring(message)
    ))
end
```
