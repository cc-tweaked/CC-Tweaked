---
module: [kind=event] http_failure
---

Fired when a request created with [http.request](module/http.html#v:request) fails. 

## Return values
1. @{string}: The event name.
2. @{string}: The url that was requested.
3. @{table}: The [responce](module/http.html#ty:Response) handle, like in a [http.get](module/http.html#v:get).

## Example
Requests the example 404 page and prints the result.

```lua
-- Request the example page
http.request("https://example.computercraft.cc/404")
while true do
  -- Wait for a http_failure event.
  local event, url, handle = os.pullEvent("http_failure")
  -- If it's for our URL, then print the result.
  if url == "https://example.computercraft.cc/404" then
    print(reason, handle and handle.readAll() or "")
    if handle then handle.close() end
  end
end
```
