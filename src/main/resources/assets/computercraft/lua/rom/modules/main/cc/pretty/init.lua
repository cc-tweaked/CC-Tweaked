local pp = require("cc.pretty.document")
local type, colours = type, colours

local keywords = {
    [ "and" ] = true, [ "break" ] = true, [ "do" ] = true, [ "else" ] = true,
    [ "elseif" ] = true, [ "end" ] = true, [ "false" ] = true, [ "for" ] = true,
    [ "function" ] = true, [ "if" ] = true, [ "in" ] = true, [ "local" ] = true,
    [ "nil" ] = true, [ "not" ] = true, [ "or" ] = true, [ "repeat" ] = true, [ "return" ] = true,
    [ "then" ] = true, [ "true" ] = true, [ "until" ] = true, [ "while" ] = true,
  }

local comma = pp.text(",")
local braces = pp.text("{}")
local obrace, cbrace = pp.text("{"), pp.text("}")
local obracket, cbracket = pp.text("["), pp.text("] = ")

local function key_compare(a, b)
    local ta, tb = type(a), type(b)

    if ta == "string" then return tb ~= "string" or a < b
    elseif tb == "string" then return false
    end

    if ta == "number" then return tb ~= "number" or a < b end
    return false
end

local function pretty_impl(obj, tracking)
    local obj_type = type(obj)
    if obj_type == "string" then
        local formatted = string.format("%q", obj):gsub("\\\n", "\\n")
        return pp.text(formatted, colours.red)
    elseif obj_type == "number" then
        return pp.text(tostring(obj), colours.magenta)
    elseif obj_type ~= "table" or tracking[obj] then
        return pp.text(tostring(obj), colours.lightGrey)
    elseif getmetatable(obj) ~= nil and getmetatable(obj).__tostring then
        return pp.text(tostring(obj))
    elseif next(obj) == nil then
        return braces
    else
        tracking[obj] = true
        local doc, n = { pp.space_line }, 1

        local length, keys, keysn = #obj, {}, 1
        for k in pairs(obj) do keys[keysn], keysn = k, keysn + 1 end
        table.sort(keys, key_compare)

        for i = 1, keysn - 1 do
            if i > 1 then
                doc[n + 1] = comma
                doc[n + 2] = pp.space_line
                n = n + 2
            end

            local k = keys[i]
            local v = obj[k]
            local ty = type(k)
            if ty == "number" and k % 1 == 0 and k >= 1 and k <= length then
                doc[n + 1], n = pretty_impl(v, tracking), n + 1
            elseif ty == "string" and not keywords[k] and string.match( k, "^[%a_][%a%d_]*$" ) then
                doc[n + 1] = pp.text(k .. " = ")
                doc[n + 2] = pretty_impl(v, tracking)
                n = n + 2
            else
                doc[n + 1] = obracket
                doc[n + 2] = pretty_impl(k, tracking)
                doc[n + 3] = cbracket
                doc[n + 4] = pretty_impl(v, tracking)
                n = n + 4
            end
        end

        tracking[obj] = nil
        return pp.group(pp.concat(obrace, pp.nest(2, pp.concat(table.unpack(doc, 1, n))), pp.space_line, cbrace))
    end
end

--- Pretty-print an arbitrary object, converting it into a document.
--
-- This can then be rendered with @{pp.write|write}.
--
-- @param obj The object to pretty-print.
-- @treturn cc.pretty.document.Doc The object formatted as a document.
local function pretty(obj)
    return pretty_impl(obj, {})
end

return {
    pretty = pretty,
    write = pp.write,
}
