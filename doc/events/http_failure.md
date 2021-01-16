---
module: [kind=event] http_failure
see: http.request To send an HTTP request.
---

The @{http_failure} event is fired when an HTTP request fails.

This event is normally handled inside @{http.get} and @{http.post}, but it can still be seen when using @{http.request}.

## Return Values
1. @{string}: The URL of the site requested.
2. @{string}: An error describing the failure.
3. @{http.Response|nil}: A response handle if the connection succeeded, but the server's response indicated failure.

## Example
Prints an error why the website cannot be contacted:
```lua
http.request("http://this.website.does.not.exist")
local event, url, err = os.pullEvent("http_failure")
print("The URL " .. url .. " could not be reached: " .. err)
```

Prints the contents of a webpage that does not exist:
```lua
http.request("http://httpbin.org/status/404")
local event, url, err, handle = os.pullEvent("http_failure")
print("The URL " .. url .. " could not be reached: " .. err)
print(handle.getResponseCode())
handle.close()
```
