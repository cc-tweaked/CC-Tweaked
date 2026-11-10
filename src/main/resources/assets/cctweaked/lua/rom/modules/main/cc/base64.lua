-- SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- The [`cc.base64`] module provides functions for converting binary data to
and from [Base64](https://en.wikipedia.org/wiki/Base64).

@usage Encode and decode a string from Base64.

    local base64 = require "cc.base64"
    print(base64.encode("Hello, world"))
    print(base64.decode("SGVsbG8sIHdvcmxk"))

@since 1.119.0
]]

local expect = require "cc.expect".expect

local rshift, byte, char, sub = bit32.rshift, string.byte, string.char, string.sub

local alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

--[[-
Encode a binary string to Base64.

@tparam string str The binary data to encode.
@tparam[opt="+/"] string alt_chars A string of length 2, used to encode the 62nd
and 63rd bit.
@treturn string The Base64 encoded data.

@usage Convert a string to Base64

    local base64 = require "cc.base64"
    print(base64.encode("Hello, world!"))

@usage Convert a string to [base64url]. This is an alternative form of Base64,
where the string is encoded with `"-_"` instead of `"+/"`. This allows the string
to be more easily used in URLs, though the padding `=` will still need escaping
with [`textutils.urlEncode`].

    local base64 = require "cc.base64"
    print(base64.encode("Test: \255\230", "-_"))

[base64url]: https://datatracker.ietf.org/doc/html/rfc4648#section-5 "Base 64 Encoding with URL and Filename Safe Alphabet"
]]
local function encode(str, alt_chars)
    expect(1, str, "string")
    expect(2, alt_chars, "string", "nil")

    if alt_chars and #alt_chars ~= 2 then
        error("alt_chars must be exactly two characters", 2)
    end

    --[[
    The below code is optimised to run against Cobalt, so the code is not
    entirely idiomatic.
     - It's quicker to build use a table lookup and do `lookup[x]` than call
       `sub(alphabet, x, x)`.
     - As we don't have bit operations, it's quicker to do `x % y`, rather than
       `band(x, y - 1)`
     - Naive concatenation is quicker than appending to a table.
    ]]

    local alphabet = alphabet .. (alt_chars or "+/")
    local lookup = {}
    for i = 1, #alphabet do lookup[i] = sub(alphabet, i, i) end

    local len = #str
    local remainder = len % 3
    local out = ""
    for i = 1, len - remainder, 3 do
        local c1, c2, c3 = byte(str, i, i + 2)
        out = out ..
            lookup[rshift(c1, 2) + 1] ..
            lookup[c1 % 4 * 16 + rshift(c2, 4) + 1] ..
            lookup[c2 % 16 * 4 + rshift(c3, 6) + 1] ..
            lookup[c3 % 64 + 1]
    end

    if remainder == 2 then
        local c1, c2 = byte(str, len - 1, len)
        out = out ..
            lookup[rshift(c1, 2) + 1] ..
            lookup[c1 % 4 * 16 + rshift(c2, 4) + 1] ..
            lookup[c2 % 16 * 4 + 1] ..
            "="
    elseif remainder == 1 then
        local c1 = byte(str, len)
        out = out .. lookup[rshift(c1, 2) + 1] .. lookup[c1 % 4 * 16 + 1] .. "=="
    end

    return out
end

--[[-
Decode a Base64-encoded string back to its original data.

This function requires the data to be valid Base64 with the trailing padding
bytes.

@tparam string str The Base64-encoded data to decode.
@tparam[opt="+/"] string alt_chars A string of length 2, used to encode the 62nd
and 63rd bit.
@treturn[1] string The decoded data.
@treturn[2] nil If the data is not valid Base64, or is missing the trailing padding.
@treturn[2] string The reason the data failed to decode.

@usage Decode a string from Base64

    local base64 = require "cc.base64"
    print(base64.decode("SGVsbG8sIHdvcmxk"))

@usage Decode [base64url]-encoded data.

    local base64 = require "cc.base64"
    print(base64.decode("VGVzdDog_-Y=", "-_"))

[base64url]: https://datatracker.ietf.org/doc/html/rfc4648#section-5 "Base 64 Encoding with URL and Filename Safe Alphabet"
]]
local function decode(str, alt_chars)
    expect(1, str, "string")
    expect(2, alt_chars, "string", "nil")

    if alt_chars and #alt_chars ~= 2 then
        error("alt_chars must be exactly two characters", 2)
    end

    if not alt_chars then alt_chars = "+/" end

    local len = #str

    if (len % 4) ~= 0 or not str:find("^[%w%" .. alt_chars:sub(1, 1) .. "%" .. alt_chars:sub(2, 2) .. "]*=?=?$") then
        return nil, "input is not valid base64"
    end

    local alphabet = alphabet .. alt_chars
    local lookup = {}
    for i = 1, #alphabet do lookup[byte(alphabet, i)] = i - 1 end

    local padding
    if sub(str, -2) == "==" then
        padding = 2
    elseif sub(str, -1) == "=" then
        padding = 1
    else
        padding = 0
    end

    local out = ""
    for i = 1, padding == 0 and len or len - 4, 4 do
        local e1, e2, e3, e4 = byte(str, i, i + 3)
        e1 = lookup[e1]
        e2 = lookup[e2]
        e3 = lookup[e3]
        e4 = lookup[e4]
        out = out .. char(
            e1 * 4 + rshift(e2, 4),
            e2 % 16 * 16 + rshift(e3, 2),
            e3 % 4 * 64 + e4
        )
    end

    if padding == 2 then
        local e1, e2 = byte(str, len - 3, len - 2)
        e1 = lookup[e1]
        e2 = lookup[e2]
        out = out .. char(e1 * 4 + rshift(e2, 4))
    elseif padding == 1 then
        local e1, e2, e3 = byte(str, len - 3, len - 1)
        e1 = lookup[e1]
        e2 = lookup[e2]
        e3 = lookup[e3]
        out = out .. char(
            e1 * 4 + rshift(e2, 4),
            e2 % 16 * 16 + rshift(e3, 2)
        )
    end

    return out
end

return { encode = encode, decode = decode }
