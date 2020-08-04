--- The @{textutils} API provides helpful utilities for formatting and
-- manipulating strings.
--
-- @module textutils

local expect = dofile("rom/modules/main/cc/expect.lua")
local expect, field = expect.expect, expect.field

--- Slowly writes string text at current cursor position,
-- character-by-character.
--
-- Like @{write}, this does not insert a newline at the end.
--
-- @tparam string sText The the text to write to the screen
-- @tparam[opt] number nRate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowWrite("Hello, world!")
-- @usage textutils.slowWrite("Hello, world!", 5)
function slowWrite(sText, nRate)
    expect(2, nRate, "number", "nil")
    nRate = nRate or 20
    if nRate < 0 then
        error("Rate must be positive", 2)
    end
    local nSleep = 1 / nRate

    sText = tostring(sText)
    local x, y = term.getCursorPos()
    local len = #sText

    for n = 1, len do
        term.setCursorPos(x, y)
        sleep(nSleep)
        local nLines = write(string.sub(sText, 1, n))
        local _, newY = term.getCursorPos()
        y = newY - nLines
    end
end

--- Slowly prints string text at current cursor position,
-- character-by-character.
--
-- Like @{print}, this inserts a newline after printing.
--
-- @tparam string sText The the text to write to the screen
-- @tparam[opt] number nRate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowPrint("Hello, world!")
-- @usage textutils.slowPrint("Hello, world!", 5)
function slowPrint(sText, nRate)
    slowWrite(sText, nRate)
    print()
end

--- Takes input time and formats it in a more readable format such as `6:30 PM`.
--
-- @tparam number nTime The time to format, as provided by @{os.time}.
-- @tparam[opt] boolean bTwentyFourHour Whether to format this as a 24-hour
-- clock (`18:30`) rather than a 12-hour one (`6:30 AM`)
-- @treturn string The formatted time
-- @usage textutils.formatTime(os.time())
function formatTime(nTime, bTwentyFourHour)
    expect(1, nTime, "number")
    expect(2, bTwentyFourHour, "boolean", "nil")
    local sTOD = nil
    if not bTwentyFourHour then
        if nTime >= 12 then
            sTOD = "PM"
        else
            sTOD = "AM"
        end
        if nTime >= 13 then
            nTime = nTime - 12
        end
    end

    local nHour = math.floor(nTime)
    local nMinute = math.floor((nTime - nHour) * 60)
    if sTOD then
        return string.format("%d:%02d %s", nHour, nMinute, sTOD)
    else
        return string.format("%d:%02d", nHour, nMinute)
    end
end

local function makePagedScroll(_term, _nFreeLines)
    local nativeScroll = _term.scroll
    local nFreeLines = _nFreeLines or 0
    return function(_n)
        for _ = 1, _n do
            nativeScroll(1)

            if nFreeLines <= 0 then
                local _, h = _term.getSize()
                _term.setCursorPos(1, h)
                _term.write("Press any key to continue")
                os.pullEvent("key")
                _term.clearLine()
                _term.setCursorPos(1, h)
            else
                nFreeLines = nFreeLines - 1
            end
        end
    end
end

--- Prints a given string to the display.
--
-- If the action can be completed without scrolling, it acts much the same as
-- @{print}; otherwise, it will throw up a "Press any key to continue" prompt at
-- the bottom of the display. Each press will cause it to scroll down and write
-- a single line more before prompting again, if need be.
--
-- @tparam string _sText The text to print to the screen.
-- @tparam[opt] number _nFreeLines The number of lines which will be
-- automatically scrolled before the first prompt appears (meaning _nFreeLines +
-- 1 lines will be printed). This can be set to the terminal's height - 2 to
-- always try to fill the screen. Defaults to 0, meaning only one line is
-- displayed before prompting.
-- @treturn number The number of lines printed.
-- @usage
-- local width, height = term.getSize()
-- textutils.pagedPrint(("This is a rather verbose dose of repetition.\n"):rep(30), height - 2)
function pagedPrint(_sText, _nFreeLines)
    expect(2, _nFreeLines, "number", "nil")
    -- Setup a redirector
    local oldTerm = term.current()
    local newTerm = {}
    for k, v in pairs(oldTerm) do
        newTerm[k] = v
    end
    newTerm.scroll = makePagedScroll(oldTerm, _nFreeLines)
    term.redirect(newTerm)

    -- Print the text
    local result
    local ok, err = pcall(function()
        if _sText ~= nil then
            result = print(_sText)
        else
            result = print()
        end
    end)

    -- Removed the redirector
    term.redirect(oldTerm)

    -- Propogate errors
    if not ok then
        error(err, 0)
    end
    return result
end

local function adjustColumnWidths(nWidth, tWidths)
    --[[
    Tries to fit all columns in tWidths on the screen width (nWidth)
    Shrinks biggest columns down to either
    1. Match the next smaller column
    2. by 1 if there are equally sized columns to alternate between the two
    3. By the amount the entire table is too wide if the column doesn't get too small
    ]]
    nWidth = nWidth - (#tWidths - 1) -- Spaces in between columns

    local nSum = 0
    -- Figure out the sum once; this well get adjusted accordingly later
    -- and thus just needs to be done once
    for _, v in pairs(tWidths) do
        nSum = nSum + v
    end

    while nSum > nWidth do
        -- Next smaller elements holds element <= nMaxElement
        -- This way we can prevent big elements being shrunk into oblivion if they are
        -- way too large but there are other big elements
        -- Both are only indices to the table
        local nMaxElement, nNextSmallerElement = 1

        for nKey, nColumnWidth in pairs(tWidths) do
            if nColumnWidth > tWidths[nMaxElement] then
                nNextSmallerElement = nMaxElement
                nMaxElement = nKey
            elseif nColumnWidth <= tWidths[nMaxElement] and (not nNextSmallerElement or nColumnWidth > tWidths[nNextSmallerElement]) then
                nNextSmallerElement = nKey
            end
        end

        -- At this point we _will_ have a max element, but _may_ only have a next smaller element
        local nShrinkAmount
        if nNextSmallerElement then
            if tWidths[nMaxElement] ~= tWidths[nNextSmallerElement] then
                nShrinkAmount = math.min(nSum - nWidth, (tWidths[nMaxElement] - tWidths[nNextSmallerElement]))
            else
                -- This will alternate between the two elements
                nShrinkAmount = 1
            end
        else
            -- This will only happen in a one element table where theres no
            -- nNextSmallerElement <= nMaxElement
            nShrinkAmount = nSum - nWidth
        end

        local nNewWidth = tWidths[nMaxElement] - nShrinkAmount
        -- Either the column contained text and would shrink to zero width
        -- or it would shrink below zero
        if nNewWidth < 1 and tWidths[nMaxElement] > 0 or nNewWidth < 0 then
            error("Unable to fit table on screen width, column " .. nMaxElement .. " would shrink too much!", 2)
        end

        tWidths[nMaxElement] = nNewWidth
        nSum = nSum - nShrinkAmount -- Adjust the sum accordingly
    end
end

local function chopUpColumns(tWidths, tInput)
    --[[
    This chops up all columns that would bee too wide to fit in the given space.
    As this would be _very_ hard/messy to do whilst printing the table out,
    it is done beforehand.
    This will be done this way:
    -> Columns that already fit will be left that way
    -> Try to fit the maximum amount of words (non-space) on one line and wrap the rest into
       the next line to be dealt with in next iteration
       -> If word would still fit without whitespaces, those will be left out and the
          rest wrapped
       -> If word itself is longer than column width, it will get chopped up
          and the rest of it wrapped into next line
    ]]

    local function insertNewLine(bInserted, tRows, nRow, nColumn, sText)
        if not bInserted then
            -- Only execute this the first time (upon creation of new line)
            table.insert(tRows, nRow, {[nColumn] = sText})
            return
        end

        tRows[nRow][nColumn] = sText
    end

    for nRow, vElement in ipairs(tInput) do
        if type(vElement) == "table" then
            -- A new row shall only be inserted once. This is needed as we are
            -- checking multiple columns that may each be too long
            local bInsertedRow = false
            local sCurrString = ""

            for nCol, sVal in pairs(vElement) do
                if #sVal > tWidths[nCol] then
                    -- Now begin splitting up the string
                    for sWord, sSpaces in sVal:gmatch("([^%s]+)(%s*)") do
                        local nWordLen = #sWord

                        if (#sCurrString + nWordLen + #sSpaces) <= tWidths[nCol] then
                            -- This entire segment will still fit
                            sCurrString = sCurrString .. sWord .. sSpaces
                        elseif (#sCurrString + nWordLen) <= tWidths[nCol] then
                            -- We can fit the word without the succeeding spaces
                            -- fit the rest on a new line (spaces will be left out)
                            sCurrString = sCurrString .. sWord

                            tInput[nRow][nCol] = sCurrString

                            -- Don't carry over the spaces (copy substring after word len + space len)
                            insertNewLine(bInsertedRow, tInput, nRow + 1, nCol, sVal:sub(#sCurrString + #sSpaces + 1))
                            bInsertedRow = true

                            break
                        else
                            if nWordLen > tWidths[nCol] then
                                -- Word needs to be split as it would not even fit on an empty column
                                -- This is so we don't endlessly try to fit it on a new line
                                sCurrString = sCurrString .. sWord:sub(1, tWidths[nCol] - #sCurrString)
                            end

                            tInput[nRow][nCol] = sCurrString
                            insertNewLine(bInsertedRow, tInput, nRow + 1, nCol, sVal:sub(#sCurrString + 1))
                            bInsertedRow = true

                            break
                        end
                    end
                end
            end
        end
    end
end

local function tabulateCommon(bPaged, ...)
    local tInput = {...}

    -- Do some first input sanitizing
    for i = 1, #tInput do
        expect(i, tInput[i], "number", "table") -- Either a color or column data
    end

    local tMaxColumnWidths = {}
    local tCopy = {}

    --[[
    This loop serves multiple purposes:
    1. Assure each row (table argument) only consists of string data
    2. Find out the maximum width for every single column
    3. Copy over the input to a new table, it will be mutated later
    ]]
    for nIndex, vElement in ipairs(tInput) do
        if type(vElement) == "table" then -- Actual column data
            tCopy[nIndex] = {}

            for nColumn, sValue in pairs(vElement) do
                if type(sValue) ~= "string" then
                    error("Bad argument #" .. nIndex .. "." .. nColumn .. " (expected string, got " .. type(sValue) .. ")", 3)
                end

                tCopy[nIndex][nColumn] = sValue -- Copy over current column data
                tMaxColumnWidths[nColumn] = math.max(tMaxColumnWidths[nColumn] or 0, #sValue)
            end
        else
            -- Copy over color
            tCopy[nIndex] = vElement
        end
    end

    local nWidth, nHeight = term.getSize()
    adjustColumnWidths(nWidth, tMaxColumnWidths)
    chopUpColumns(tMaxColumnWidths, tCopy)

    local _, yPos = term.getCursorPos()
    local nAvailableLines = math.max(nHeight - yPos, 0) -- Maybe, for some reason, cursor was below screen

    for _, nRow in ipairs(tCopy) do
        if type(nRow) == "table" then
            if nAvailableLines <= 0 then
                if bPaged then
                    local nFGColor = term.getTextColor()

                    term.setTextColor(colors.white)
                    term.setCursorPos(1, nHeight)
                    term.write("Press any key to continue")
                    term.setTextColor(nFGColor)

                    os.pullEvent("key")
                    term.clearLine()
                end

                term.scroll(1)
                nAvailableLines = nAvailableLines + 1
            end

            local xPos = 1

            for i = 1, #tMaxColumnWidths do
                if nRow[i] then
                    term.setCursorPos(xPos, nHeight - nAvailableLines)
                    term.write(nRow[i])
                end

                xPos = xPos + tMaxColumnWidths[i] + 1
            end

            nAvailableLines = nAvailableLines - 1
        else
            term.setTextColor(nRow)
        end
    end

    -- Jump to next line so the cursor doesn't stay behind last written column
    term.setCursorPos(1, nHeight - nAvailableLines)
end

--- Prints tables in a structured form.
--
-- This accepts multiple arguments, either a table or a number. When
-- encountering a table, this will be treated as a table row, with each column
-- width being auto-adjusted.
--
-- When encountering a number, this sets the text color of the subsequent rows to it.
--
-- @tparam {string...}|number ... The rows and text colors to display.
-- @usage textutils.tabulate(colors.orange, { "1", "2", "3" }, colors.lightBlue, { "A", "B", "C" })
function tabulate(...)
    return tabulateCommon(false, ...)
end

--- Prints tables in a structured form, stopping and prompting for input should
-- the result not fit on the terminal.
--
-- This functions identically to @{textutils.tabulate}, but will prompt for user
-- input should the whole output not fit on the display.
--
-- @tparam {string...}|number ... The rows and text colors to display.
-- @usage textutils.tabulate(colors.orange, { "1", "2", "3" }, colors.lightBlue, { "A", "B", "C" })
-- @see textutils.tabulate
-- @see textutils.pagedPrint
function pagedTabulate(...)
    return tabulateCommon(true, ...)
end

local g_tLuaKeywords = {
    ["and"] = true,
    ["break"] = true,
    ["do"] = true,
    ["else"] = true,
    ["elseif"] = true,
    ["end"] = true,
    ["false"] = true,
    ["for"] = true,
    ["function"] = true,
    ["if"] = true,
    ["in"] = true,
    ["local"] = true,
    ["nil"] = true,
    ["not"] = true,
    ["or"] = true,
    ["repeat"] = true,
    ["return"] = true,
    ["then"] = true,
    ["true"] = true,
    ["until"] = true,
    ["while"] = true,
}

local function serializeImpl(t, tTracking, sIndent)
    local sType = type(t)
    if sType == "table" then
        if tTracking[t] ~= nil then
            error("Cannot serialize table with recursive entries", 0)
        end
        tTracking[t] = true

        if next(t) == nil then
            -- Empty tables are simple
            return "{}"
        else
            -- Other tables take more work
            local sResult = "{\n"
            local sSubIndent = sIndent .. "  "
            local tSeen = {}
            for k, v in ipairs(t) do
                tSeen[k] = true
                sResult = sResult .. sSubIndent .. serializeImpl(v, tTracking, sSubIndent) .. ",\n"
            end
            for k, v in pairs(t) do
                if not tSeen[k] then
                    local sEntry
                    if type(k) == "string" and not g_tLuaKeywords[k] and string.match(k, "^[%a_][%a%d_]*$") then
                        sEntry = k .. " = " .. serializeImpl(v, tTracking, sSubIndent) .. ",\n"
                    else
                        sEntry = "[ " .. serializeImpl(k, tTracking, sSubIndent) .. " ] = " .. serializeImpl(v, tTracking, sSubIndent) .. ",\n"
                    end
                    sResult = sResult .. sSubIndent .. sEntry
                end
            end
            sResult = sResult .. sIndent .. "}"
            return sResult
        end

    elseif sType == "string" then
        return string.format("%q", t)

    elseif sType == "number" or sType == "boolean" or sType == "nil" then
        return tostring(t)

    else
        error("Cannot serialize type " .. sType, 0)

    end
end

local function mk_tbl(str, name)
    local msg = "attempt to mutate textutils." .. name
    return setmetatable({}, {
        __newindex = function() error(msg, 2) end,
        __tostring = function() return str end,
    })
end

--- A table representing an empty JSON array, in order to distinguish it from an
-- empty JSON object.
--
-- The contents of this table should not be modified.
--
-- @usage textutils.serialiseJSON(textutils.empty_json_array)
-- @see textutils.serialiseJSON
-- @see textutils.unserialiseJSON
empty_json_array = mk_tbl("[]", "empty_json_array")

--- A table representing the JSON null value.
--
-- The contents of this table should not be modified.
--
-- @usage textutils.serialiseJSON(textutils.json_null)
-- @see textutils.serialiseJSON
-- @see textutils.unserialiseJSON
json_null = mk_tbl("null", "json_null")

local function serializeJSONImpl(t, tTracking, bNBTStyle)
    local sType = type(t)
    if t == empty_json_array then return "[]"
    elseif t == json_null then return "null"

    elseif sType == "table" then
        if tTracking[t] ~= nil then
            error("Cannot serialize table with recursive entries", 0)
        end
        tTracking[t] = true

        if next(t) == nil then
            -- Empty tables are simple
            return "{}"
        else
            -- Other tables take more work
            local sObjectResult = "{"
            local sArrayResult = "["
            local nObjectSize = 0
            local nArraySize = 0
            for k, v in pairs(t) do
                if type(k) == "string" then
                    local sEntry
                    if bNBTStyle then
                        sEntry = tostring(k) .. ":" .. serializeJSONImpl(v, tTracking, bNBTStyle)
                    else
                        sEntry = string.format("%q", k) .. ":" .. serializeJSONImpl(v, tTracking, bNBTStyle)
                    end
                    if nObjectSize == 0 then
                        sObjectResult = sObjectResult .. sEntry
                    else
                        sObjectResult = sObjectResult .. "," .. sEntry
                    end
                    nObjectSize = nObjectSize + 1
                end
            end
            for _, v in ipairs(t) do
                local sEntry = serializeJSONImpl(v, tTracking, bNBTStyle)
                if nArraySize == 0 then
                    sArrayResult = sArrayResult .. sEntry
                else
                    sArrayResult = sArrayResult .. "," .. sEntry
                end
                nArraySize = nArraySize + 1
            end
            sObjectResult = sObjectResult .. "}"
            sArrayResult = sArrayResult .. "]"
            if nObjectSize > 0 or nArraySize == 0 then
                return sObjectResult
            else
                return sArrayResult
            end
        end

    elseif sType == "string" then
        return string.format("%q", t)

    elseif sType == "number" or sType == "boolean" then
        return tostring(t)

    else
        error("Cannot serialize type " .. sType, 0)

    end
end

local unserialise_json
do
    local sub, find, match, concat, tonumber = string.sub, string.find, string.match, table.concat, tonumber

    --- Skip any whitespace
    local function skip(str, pos)
        local _, last = find(str, "^[ \n\r\v]+", pos)
        if last then return last + 1 else return pos end
    end

    local escapes = {
        ["b"] = '\b', ["f"] = '\f', ["n"] = '\n', ["r"] = '\r', ["t"] = '\t',
        ["\""] = "\"", ["/"] = "/", ["\\"] = "\\",
    }

    local mt = {}

    local function error_at(pos, msg, ...)
        if select('#', ...) > 0 then msg = msg:format(...) end
        error(setmetatable({ pos = pos, msg = msg }, mt))
    end

    local function expected(pos, actual, exp)
        if actual == "" then actual = "end of input" else actual = ("%q"):format(actual) end
        error_at(pos, "Unexpected %s, expected %s.", actual, exp)
    end

    local function parse_string(str, pos)
        local buf, n = {}, 1

        while true do
            local c = sub(str, pos, pos)
            if c == "" then error_at(pos, "Unexpected end of input, expected '\"'.") end
            if c == '"' then break end

            if c == '\\' then
                -- Handle the various escapes
                c = sub(str, pos + 1, pos + 1)
                if c == "" then error_at(pos, "Unexpected end of input, expected escape sequence.") end

                if c == "u" then
                    local num_str = match(str, "^%x%x%x%x", pos + 2)
                    if not num_str then error_at(pos, "Malformed unicode escape %q.", sub(str, pos + 2, pos + 5)) end
                    buf[n], n, pos = utf8.char(tonumber(num_str, 16)), n + 1, pos + 6
                else
                    local unesc = escapes[c]
                    if not unesc then error_at(pos + 1, "Unknown escape character %q.", unesc) end
                    buf[n], n, pos = unesc, n + 1, pos + 2
                end
            elseif c >= '\x20' then
                buf[n], n, pos = c, n + 1, pos + 1
            else
                error_at(pos + 1, "Unescaped whitespace %q.", c)
            end
        end

        return concat(buf, "", 1, n - 1), pos + 1
    end

    local valid = { b = true, B = true, s = true, S = true, l = true, L = true, f = true, F = true, d = true, D = true }
    local function parse_number(str, pos, opts)
        local _, last, num_str = find(str, '^(-?%d+%.?%d*[eE]?[+-]?%d*)', pos)
        local val = tonumber(num_str)
        if not val then error_at(pos, "Malformed number %q.", num_str) end

        if opts.nbt_style and valid[sub(str, pos + 1, pos + 1)] then return val, last + 2 end

        return val, last + 1
    end

    local function parse_ident(str, pos)
        local _, last, val = find(str, '^([%a][%w_]*)', pos)
        return val, last + 1
    end

    local function decode_impl(str, pos, opts)
        local c = sub(str, pos, pos)
        if c == '"' then return parse_string(str, pos + 1)
        elseif c == "-" or c >= "0" and c <= "9" then return parse_number(str, pos, opts)
        elseif c == "t" then
            if sub(str, pos + 1, pos + 3) == "rue" then return true, pos + 4 end
        elseif c == 'f' then
            if sub(str, pos + 1, pos + 4) == "alse" then return false, pos + 5 end
        elseif c == 'n' then
            if sub(str, pos + 1, pos + 3) == "ull" then
                if opts.parse_null then
                    return json_null, pos + 4
                else
                    return nil, pos + 4
                end
            end
        elseif c == "{" then
            local obj = {}

            pos = skip(str, pos + 1)
            c = sub(str, pos, pos)

            if c == "" then return error_at(pos, "Unexpected end of input, expected '}'.") end
            if c == "}" then return obj, pos + 1 end

            while true do
                local key, value
                if c == "\"" then key, pos = parse_string(str, pos + 1)
                elseif opts.nbt_style then key, pos = parse_ident(str, pos)
                else return expected(pos, c, "object key")
                end

                pos = skip(str, pos)

                c = sub(str, pos, pos)
                if c ~= ":" then return expected(pos, c, "':'") end

                value, pos = decode_impl(str, skip(str, pos + 1), opts)
                obj[key] = value

                -- Consume the next delimiter
                pos = skip(str, pos)
                c = sub(str, pos, pos)
                if c == "}" then break
                elseif c == "," then pos = skip(str, pos + 1)
                else return expected(pos, c, "',' or '}'")
                end

                c = sub(str, pos, pos)
            end

            return obj, pos + 1

        elseif c == "[" then
            local arr, n = {}, 1

            pos = skip(str, pos + 1)
            c = sub(str, pos, pos)

            if c == "" then return expected(pos, c, "']'") end
            if c == "]" then return empty_json_array, pos + 1 end

            while true do
                n, arr[n], pos = n + 1, decode_impl(str, pos, opts)

                -- Consume the next delimiter
                pos = skip(str, pos)
                c = sub(str, pos, pos)
                if c == "]" then break
                elseif c == "," then pos = skip(str, pos + 1)
                else return expected(pos, c, "',' or ']'")
                end
            end

            return arr, pos + 1
        elseif c == "" then error_at(pos, 'Unexpected end of input.')
        end

        error_at(pos, "Unexpected character %q.", c)
    end

    --- Converts a serialised JSON string back into a reassembled Lua object.
    --
    -- This may be used with @{textutils.serializeJSON}, or when communicating
    -- with command blocks or web APIs.
    --
    -- @tparam string s The serialised string to deserialise.
    -- @tparam[opt] { nbt_style? = boolean, parse_null? = boolean } options
    -- Options which control how this JSON object is parsed.
    --
    --  - `nbt_style`: When true, this will accept [stringified NBT][nbt] strings,
    --    as produced by many commands.
    --  - `parse_null`: When true, `null` will be parsed as @{json_null}, rather
    --    than `nil`.
    --
    --  [nbt]: https://minecraft.gamepedia.com/NBT_format
    -- @return[1] The deserialised object
    -- @treturn[2] nil If the object could not be deserialised.
    -- @treturn string A message describing why the JSON string is invalid.
    unserialise_json = function(s, options)
        expect(1, s, "string")
        expect(2, options, "table", "nil")

        if options then
            field(options, "nbt_style", "boolean", "nil")
            field(options, "nbt_style", "boolean", "nil")
        else
            options = {}
        end

        local ok, res, pos = pcall(decode_impl, s, skip(s, 1), options)
        if not ok then
            if type(res) == "table" and getmetatable(res) == mt then
                return nil, ("Malformed JSON at position %d: %s"):format(res.pos, res.msg)
            end

            error(res, 0)
        end

        pos = skip(s, pos)
        if pos <= #s then
            return nil, ("Malformed JSON at position %d: Unexpected trailing character %q."):format(pos, sub(s, pos, pos))
        end
        return res

    end
end

--- Convert a Lua object into a textual representation, suitable for
-- saving in a file or pretty-printing.
--
-- @param t The object to serialise
-- @treturn string The serialised representation
-- @throws If the object contains a value which cannot be
-- serialised. This includes functions and tables which appear multiple
-- times.
function serialize(t)
    local tTracking = {}
    return serializeImpl(t, tTracking, "")
end

serialise = serialize -- GB version

--- Converts a serialised string back into a reassembled Lua object.
--
-- This is mainly used together with @{textutils.serialize}.
--
-- @tparam string s The serialised string to deserialise.
-- @return[1] The deserialised object
-- @treturn[2] nil If the object could not be deserialised.
function unserialize(s)
    expect(1, s, "string")
    local func = load("return " .. s, "unserialize", "t", {})
    if func then
        local ok, result = pcall(func)
        if ok then
            return result
        end
    end
    return nil
end

unserialise = unserialize -- GB version

--- Returns a JSON representation of the given data.
--
-- This function attempts to guess whether a table is a JSON array or
-- object. However, empty tables are assumed to be empty objects - use
-- @{textutils.empty_json_array} to mark an empty array.
--
-- This is largely intended for interacting with various functions from the
-- @{commands} API, though may also be used in making @{http} requests.
--
-- @param t The value to serialise. Like @{textutils.serialise}, this should not
-- contain recursive tables or functions.
-- @tparam[opt] boolean bNBTStyle Whether to produce NBT-style JSON (non-quoted keys)
-- instead of standard JSON.
-- @treturn string The JSON representation of the input.
-- @throws If the object contains a value which cannot be
-- serialised. This includes functions and tables which appear multiple
-- times.
-- @usage textutils.serializeJSON({ values = { 1, "2", true } })
function serializeJSON(t, bNBTStyle)
    expect(1, t, "table", "string", "number", "boolean")
    expect(2, bNBTStyle, "boolean", "nil")
    local tTracking = {}
    return serializeJSONImpl(t, tTracking, bNBTStyle or false)
end

serialiseJSON = serializeJSON -- GB version

unserializeJSON = unserialise_json
unserialiseJSON = unserialise_json

--- Replaces certain characters in a string to make it safe for use in URLs or POST data.
--
-- @tparam string str The string to encode
-- @treturn string The encoded string.
-- @usage print("https://example.com/?view=" .. textutils.urlEncode(read()))
function urlEncode(str)
    expect(1, str, "string")
    if str then
        str = string.gsub(str, "\n", "\r\n")
        str = string.gsub(str, "([^A-Za-z0-9 %-%_%.])", function(c)
            local n = string.byte(c)
            if n < 128 then
                -- ASCII
                return string.format("%%%02X", n)
            else
                -- Non-ASCII (encode as UTF-8)
                return
                    string.format("%%%02X", 192 + bit32.band(bit32.arshift(n, 6), 31)) ..
                    string.format("%%%02X", 128 + bit32.band(n, 63))
            end
        end)
        str = string.gsub(str, " ", "+")
    end
    return str
end

local tEmpty = {}

--- Provides a list of possible completions for a partial Lua expression.
--
-- If the completed element is a table, suggestions will have `.` appended to
-- them. Similarly, functions have `(` appended to them.
--
-- @tparam string sSearchText The partial expression to complete, such as a
-- variable name or table index.
--
-- @tparam[opt] table tSearchTable The table to find variables in, defaulting to
-- the global environment (@{_G}). The function also searches the "parent"
-- environment via the `__index` metatable field.
--
-- @treturn { string... } The (possibly empty) list of completions.
-- @see shell.setCompletionFunction
-- @see read
-- @usage textutils.complete( "pa", getfenv() )
function complete(sSearchText, tSearchTable)
    expect(1, sSearchText, "string")
    expect(2, tSearchTable, "table", "nil")

    if g_tLuaKeywords[sSearchText] then return tEmpty end
    local nStart = 1
    local nDot = string.find(sSearchText, ".", nStart, true)
    local tTable = tSearchTable or _ENV
    while nDot do
        local sPart = string.sub(sSearchText, nStart, nDot - 1)
        local value = tTable[sPart]
        if type(value) == "table" then
            tTable = value
            nStart = nDot + 1
            nDot = string.find(sSearchText, ".", nStart, true)
        else
            return tEmpty
        end
    end
    local nColon = string.find(sSearchText, ":", nStart, true)
    if nColon then
        local sPart = string.sub(sSearchText, nStart, nColon - 1)
        local value = tTable[sPart]
        if type(value) == "table" then
            tTable = value
            nStart = nColon + 1
        else
            return tEmpty
        end
    end

    local sPart = string.sub(sSearchText, nStart)
    local nPartLength = #sPart

    local tResults = {}
    local tSeen = {}
    while tTable do
        for k, v in pairs(tTable) do
            if not tSeen[k] and type(k) == "string" then
                if string.find(k, sPart, 1, true) == 1 then
                    if not g_tLuaKeywords[k] and string.match(k, "^[%a_][%a%d_]*$") then
                        local sResult = string.sub(k, nPartLength + 1)
                        if nColon then
                            if type(v) == "function" then
                                table.insert(tResults, sResult .. "(")
                            elseif type(v) == "table" then
                                local tMetatable = getmetatable(v)
                                if tMetatable and (type(tMetatable.__call) == "function" or  type(tMetatable.__call) == "table") then
                                    table.insert(tResults, sResult .. "(")
                                end
                            end
                        else
                            if type(v) == "function" then
                                sResult = sResult .. "("
                            elseif type(v) == "table" and next(v) ~= nil then
                                sResult = sResult .. "."
                            end
                            table.insert(tResults, sResult)
                        end
                    end
                end
            end
            tSeen[k] = true
        end
        local tMetatable = getmetatable(tTable)
        if tMetatable and type(tMetatable.__index) == "table" then
            tTable = tMetatable.__index
        else
            tTable = nil
        end
    end

    table.sort(tResults)
    return tResults
end
