---
module: [kind=event] http_success
see: http.request To make an HTTP request.
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`http_success`] event is fired when an HTTP request returns successfully.

This event is normally handled inside [`http.get`] and [`http.post`], but it can still be seen when using [`http.request`].

## Return Values
1. [`string`]: The event name.
2. [`string`]: The URL of the site requested.
3. [`http.Response`]: The successful HTTP response.

## Example
Prints the content of a website (this may fail if the request fails):
```lua
local myURL = "https://tweaked.cc/"
http.request(myURL)
local event, url, handle
repeat
    event, url, handle = os.pullEvent("http_success")
until url == myURL
print("Contents of " .. url .. ":")
print(handle.readAll())
handle.close()
```
