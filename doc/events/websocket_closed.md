---
module: [kind=event] websocket_closed
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`websocket_closed`] event is fired when an open WebSocket connection is closed.

## Return Values
1. [`string`]: The event name.
2. [`string`]: The URL of the WebSocket that was closed.
3. <span class="type">[`string`]|[`nil`]</span>: The [server-provided reason][close_reason]
   the websocket was closed. This will be [`nil`] if the connection was closed
   abnormally.
4. <span class="type">[`number`]|[`nil`]</span>: The [connection close code][close_code],
   indicating why the socket was closed. This will be [`nil`] if the connection
   was closed abnormally.

[close_reason]: https://www.rfc-editor.org/rfc/rfc6455.html#section-7.1.6 "The WebSocket Connection Close Reason, RFC 6455"
[close_code]: https://www.rfc-editor.org/rfc/rfc6455.html#section-7.1.5 "The WebSocket Connection Close Code, RFC 6455"

## Example
Prints a message when a WebSocket is closed (this may take a minute):
```lua
local myURL = "wss://example.tweaked.cc/echo"
local ws = http.websocket(myURL)
local event, url
repeat
    event, url = os.pullEvent("websocket_closed")
until url == myURL
print("The WebSocket at " .. url .. " was closed.")
```
