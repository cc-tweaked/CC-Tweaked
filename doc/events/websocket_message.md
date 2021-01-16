---
module: [kind=event] websocket_message
---

The @{websocket_message} event is fired when a message is received on an open WebSocket connection.

This event is normally handled by @{Websocket.receive}, but it can also be pulled manually.

## Return Values
1. @{string}: The URL of the WebSocket.
2. @{string}: The contents of the message.

## Example
Prints a message sent by a WebSocket:
```lua
local ws = http.websocket("ws://echo.websocket.org")
ws.send("Hello!")
local event, url, message = os.pullEvent("websocket_message")
print("Received message from " .. url .. " with contents " .. message)
ws.close()
```
