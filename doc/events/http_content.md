---
module: [kind=event] http_content
see: http.request To send an HTTP request.
see: http.StreamingResponse
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`http_content`] event is fired when a [streaming HTTP request][`http.StreamingResponse`] receives new content from
the server.

This event is normally consumed by the functions on [`http.StreamingResponse`] (e.g. [`http.StreamingResponse.read`] and
[`http.StreamingResponse.readLine`]). However, when reading in non-blocking mode, this event may be used to in order
to determine when the response body may be read from again.

## Return Values
1. [`string`]: The event name.
2. [`string`]: The URL of the site requested.

## Example
Reads 16 characters at a time from the body.

```lua
local url = "https://example.tweaked.cc/stream"
local response = assert(http.get { url = url, streaming = true })

while true do
    -- Read at most 1024 bytes using non-blocking mode
    local body = response.read(1024, false)

    if body == nil then
        -- We've read all content, exit.
        break
    elseif #body == 0 then
        -- Our buffer is empty, wait for a http_content event.
        os.pullEvent("http_content")
    else
        -- Otherwise write the content we've read.
        write(body)
    end
end

response.close()
```
