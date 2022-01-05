---
module: [kind=event] websocket_success
see: http.websocketAsync To open a WebSocket asynchronously.
---

The @{websocket_success} event is fired when a WebSocket connection request returns successfully.

This event is normally handled inside @{http.websocket}, but it can still be seen when using @{http.websocketAsync}.

## Return Values
1. @{string}: The event name.
2. @{string}: The URL of the site.
3. @{http.Websocket}: The handle for the WebSocket.

## Example
Prints the content of a website (this may fail if the request fails):
```lua
local myURL = "wss://example.tweaked.cc/echo"
http.websocketAsync(myURL)
local event, url, handle
repeat
    event, url, handle = os.pullEvent("websocket_success")
until url == myURL
print("Connected to " .. url)
handle.send("Hello!")
print(handle.receive())
handle.close()
```
