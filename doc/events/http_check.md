---
module: [kind=event] http_check
see: http.checkURLAsync To check a URL asynchronously.
---

The @{http_check} event is fired when a URL check finishes.

This event is normally handled inside @{http.checkURL}, but it can still be seen when using @{http.checkURLAsync}.

## Return Values
1. @{string}: The URL requested to be checked.
2. @{boolean}: Whether the check succeeded.
3. @{string|nil}: If the check failed, a reason explaining why the check failed.

## Example
Checks a valid URL:
```lua
http.checkURLAsync("http://www.example.com")
local event, url, success, err = os.pullEvent("http_check")
print("The check on " .. url .. " " .. (success and "succeeded." or "failed: " .. err))
```

Checks an invalid URL:
```lua
http.checkURLAsync("ht:/")
local event, url, success, err = os.pullEvent("http_check")
print("The check on " .. url .. " " .. (success and "succeeded." or "failed: " .. err))
```
