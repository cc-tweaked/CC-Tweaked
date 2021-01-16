---
module: [kind=event] websocket_closed
---

The @{websocket_closed} event is fired when an open WebSocket connection is closed.

## Return Values
1. @{string}: The URL of the WebSocket that was closed.

## Example
Prints a message when a WebSocket is closed (this may take a minute):
```lua
local ws = http.websocket("ws://echo.websocket.org")
local event, url = os.pullEvent("websocket_closed")
print("The WebSocket at " .. url .. " was closed.")
```
