---
module: [kind=event] http_success
see: http.request To make an HTTP request.
---

The @{http_success} event is fired when an HTTP request returns successfully.

This event is normally handled inside @{http.get} and @{http.post}, but it can still be seen when using @{http.request}.

## Return Values
1. @{string}: The URL of the site requested.
2. @{Response}: The handle for the response text.

## Example
Prints the content of a website (this may fail if the request fails):
```lua
http.request("http://www.example.com")
local event, url, handle = os.pullEvent("http_success")
print("Contents of " .. url .. ":")
print(handle.readAll())
handle.close()
```
