-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Make HTTP requests, sending and receiving data to a remote web server.

@module http
@since 1.1
@see local_ips To allow accessing servers running on your local network.
]]

local expect = dofile("rom/modules/main/cc/expect.lua").expect

local native = http
local nativeHTTPRequest = http.request

request = http.request

local function wrap_request(_url, ...)
    nativeHTTPRequest(...)
    while true do
        local event, param1, param2, param3 = os.pullEvent()
        if event == "http_success" and param1 == _url then
            return param2
        elseif event == "http_failure" and param1 == _url then
            return nil, param2, param3
        end
    end
end

--[[- Make a HTTP GET request to the given url.

@tparam string url   The url to request

@treturn Response The resulting http response, which can be read from.
@treturn[2] nil When the http request failed, such as in the event of a 404
error or connection timeout.
@treturn string A message detailing why the request failed.

@usage Make a request to [example.tweaked.cc](https://example.tweaked.cc),
and print the returned page.

```lua
local request = http.get("https://example.tweaked.cc")
print(request.readAll())
-- => HTTP is working!
request.close()
```
]]
function get(_url)
    expect(1, _url, "string")
    return wrap_request(_url, _url)
end

--[[- Make a HTTP POST request to the given url.

@tparam string url   The url to request
@tparam string body  The body of the POST request.

@treturn Response The resulting http response, which can be read from.
@treturn[2] nil When the http request failed, such as in the event of a 404
error or connection timeout.
@treturn string A message detailing why the request failed.

@since 1.31
]]
function post(_url, _post)
    expect(1, _url, "string")
    expect(2, _post, "string")
    return wrap_request(_url, _url, _post)
end
