---
module: [kind=event] websocket_message
---

The @{websocket_message} event is fired when a message is received on an open WebSocket connection.

This event is normally handled by @{http.Websocket.receive}, but it can also be pulled manually.

## Return Values
1. @{string}: The event name.
2. @{string}: The URL of the WebSocket.
3. @{string}: The contents of the message.

## Example
Prints a message sent by a WebSocket:
```lua
local myURL = "ws://echo.websocket.org"
local ws = http.websocket(myURL)
ws.send("Hello!")
local event, url, message
repeat
    event, url, message = os.pullEvent("websocket_message")
until url == myURL
print("Received message from " .. url .. " with contents " .. message)
ws.close()
```
