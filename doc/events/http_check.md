---
module: [kind=event] http_check
---

Occurs after a successful call to [http.checkURLAsync](module/http.html#v:checkURLAsync), containing information about whether the URL can be requested or not.

## Return values
1. @{string}: The event name.
2. @{string}: The url that was checked.
3. @{boolean}: Whether the url is requestable (@{true}) or not.
4. @{string}: A reason why this URL can not be requested.

## Example
Checks a URL and waits for a http_check event, printing an error if it is blacklisted.

```lua
local check_url = "https://example.computercraft.cc/"
local ok, err = http.checkURLAsync(check_url)
if not ok then 
  printError(err)
else
  while true do
    local event, url, ok, err = os.pullEvent("http_check")
    if url == check_url then
      if not ok then printError(err) end
      break
    end
  end
end
```
