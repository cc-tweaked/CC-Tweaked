---
module: [kind=event] websocket_success
see: http.websocketAsync To open a WebSocket asynchronously.
---

The @{websocket_success} event is fired when a WebSocket connection request returns successfully.

This event is normally handled inside @{http.websocket}, but it can still be seen when using @{http.websocketAsync}.

## Return Values
1. @{string}: The URL of the site.
2. @{Websocket}: The handle for the WebSocket.

## Example
Prints the content of a website (this may fail if the request fails):
```lua
http.websocketAsync("http://echo.websocket.org")
local event, url, handle = os.pullEvent("websocket_success")
print("Connected to " .. url)
handle.send("Hello!")
print(handle.receive())
handle.close()
```
