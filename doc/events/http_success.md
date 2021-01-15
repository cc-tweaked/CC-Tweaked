---
module: [kind=event] http_success
---

Occurs when a HTTP request has completed successfully. Fired by a [http.request](module/http.html#v:request) call. 

## Return values
1. @{string}: The event name.
2. @{string}: The url that was requested.
3. @{table}: The [responce](module/http.html#ty:Response) handle, like in a [http.get](module/http.html#v:get).

## Example
Requests the example page and prints the result.

```lua
-- Request the example page
http.request("https://example.computercraft.cc")
while true do
  -- Wait for a http_success event.
  local event, url, handle = os.pullEvent("http_success")
  -- If it's for our URL, then print the result.
  if url == "https://example.computercraft.cc" then
    print(handle.readAll())
    handle.close()
  end
end
```
