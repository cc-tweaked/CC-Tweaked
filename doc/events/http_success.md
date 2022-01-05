---
module: [kind=event] http_success
see: http.request To make an HTTP request.
---

The @{http_success} event is fired when an HTTP request returns successfully.

This event is normally handled inside @{http.get} and @{http.post}, but it can still be seen when using @{http.request}.

## Return Values
1. @{string}: The event name.
2. @{string}: The URL of the site requested.
3. @{http.Response}: The handle for the response text.

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
