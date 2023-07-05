local expect = (require and require("cc.expect") or dofile("rom/modules/main/cc/expect.lua"))
local utflib = (require and require("cc.utflib") or dofile("rom/modules/main/cc/utflib.lua"))
local expect, field = expect.expect, expect.field
local wrap = (require and require("cc.utflib.strings") or dofile("rom/modules/main/cc/utflib/strings.lua")).wrap
local uterm = (require and require("cc.utflib.term") or dofile("rom/modules/main/cc/utflib/term.lua"))

local write, print = uterm.write, uterm.print

local utextutils = {}

--- Slowly writes string text at current cursor position,
-- character-by-character.
--
-- Like @{_G.write}, this does not insert a newline at the end.
--
-- @tparam string text The the text to write to the screen
-- @tparam[opt] number rate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowWrite("Hello, world!")
-- @usage textutils.slowWrite("Hello, world!", 5)
-- @since 1.3
function utextutils.slowWrite(text, rate)
    expect(2, rate, "number", "nil")
    rate = rate or 20
    if rate < 0 then
        error("Rate must be positive", 2)
    end
    if not utflib.isUTFString(text) and type(text) ~= 'string' then
        text = tostring(text)
    end
    local to_sleep = 1 / rate

    local wrapped_lines = wrap(text, (term.getSize()))
    local wrapped_str = table.concat(wrapped_lines, "\n")

    for n = 1, #wrapped_str do
        sleep(to_sleep)
        write(wrapped_str:sub(n, n))
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
function utextutils.slowPrint(sText, nRate)
    slowWrite(sText, nRate)
    print()
end

--- Takes input time and formats it in a more readable format such as `6:30 PM`.
-- differ from the normal version, this accepts custom 12-hour format.
--
-- @tparam number nTime The time to format, as provided by @{os.time}.
-- @tparam[opt] boolean bTwentyFourHour Whether to format this as a 24-hour
-- clock (`18:30`) rather than a 12-hour one (`6:30 AM`)
-- @tparam[opt] string amFormat format for time before noon. default to `%s AM`. can be UTFString
-- @tparam[opt] string pmFormat format for time after noon. default to `%s PM`. can be UTFString
-- @treturn string The formatted time
-- @usage Print the current in-game time as a 12-hour clock.
--
--     textutils.formatTime(os.time())
-- @usage Print the local time as a 24-hour clock.
--
--     textutils.formatTime(os.time("local"), true)
function utextutils.formatTime(nTime, bTwentyFourHour, amFormat, pmFormat)
    expect(1, nTime, "number")
    expect(2, bTwentyFourHour, "boolean", "nil")
    if not isUTFString(amFormat) then expect(3, amFormat, "string", "nil") end
    if not isUTFString(pmFormat) then expect(4, pmFormat, "string", "nil") end
    local sTOD = nil
    if not bTwentyFourHour then
        if nTime >= 12 then
            sTOD = utflib.UTFString(pmFormat or "%s PM")
        else
            sTOD = utflib.UTFString(amFormat or "%s AM")
        end
        if nTime >= 13 then
            nTime = nTime - 12
        end
    end

    local nHour = math.floor(nTime)
    local nMinute = math.floor((nTime - nHour) * 60)
    if sTOD then
        return sTOD:format(string.format("%d:%02d", nHour == 0 and 12 or nHour, nMinute))
    else
        return utflib.UTFString(string.format("%d:%02d", nHour, nMinute))
    end
end

local function makePagedScroll(_term, _nFreeLines, _cont_hint)
    local nativeScroll = _term.scroll
    local nFreeLines = _nFreeLines or 0
    _cont_hint = utflib.UTFString(_cont_hint or "Press any key to continue")
    return function(_n)
        for _ = 1, _n do
            nativeScroll(1)

            if nFreeLines <= 0 then
                local _, h = _term.getSize()
                _term.setCursorPos(1, h)
                _term._writeutf8(_cont_hint)
                os.pullEvent("key")
                _term.clearLine()
                _term.setCursorPos(1, h)
            else
                nFreeLines = nFreeLines - 1
            end
        end
    end
end

--[[- Prints a given string to the display.

If the action can be completed without scrolling, it acts much the same as
@{print}; otherwise, it will throw up a "Press any key to continue" prompt at
the bottom of the display. Each press will cause it to scroll down and write a
single line more before prompting again, if need be.
Differ from the normal version, this allows customization on the continue prompt.

@tparam string text The text to print to the screen.
@tparam[opt] number free_lines The number of lines which will be
automatically scrolled before the first prompt appears (meaning free_lines +
1 lines will be printed). This can be set to the cursor's y position - 2 to
always try to fill the screen. Defaults to 0, meaning only one line is
displayed before prompting.
@tparam[opt] string contHint The continue prompt shown if the action cannot be
completed without scrolling. Use the default prompt when not provided. Can be UTFString.
@treturn number The number of lines printed.

@usage Generates several lines of text and then prints it, paging once the
bottom of the terminal is reached.

    local lines = {}
    for i = 1, 30 do lines[i] = ("This is line #%d"):format(i) end
    local message = table.concat(lines, "\n")

    local width, height = term.getCursorPos()
    textutils.pagedPrint(message, height - 2)
]]
function utextutils.pagedPrint(text, free_lines, contHint)
    expect(2, free_lines, "number", "nil")
    if not utflib.isUTFString(contHint) then expect(3, contHint, "string", "nil") end
    -- Setup a redirector
    local oldTerm = term.current()
    local newTerm = {}
    for k, v in pairs(oldTerm) do
        newTerm[k] = v
    end

    newTerm.scroll = makePagedScroll(oldTerm, free_lines, contHint)
    term.redirect(newTerm)

    -- Print the text
    local result
    local ok, err = pcall(function()
        if text ~= nil then
            result = print(text)
        else
            result = print()
        end
    end)

    -- Removed the redirector
    term.redirect(oldTerm)

    -- Propagate errors
    if not ok then
        error(err, 0)
    end
    return result
end

local function tabulateCommon(bPaged, ...)
    local tAll = table.pack(...)
    for i = 1, tAll.n do
        expect(i, tAll[i], "number", "table")
    end

    local w, h = term.getSize()
    local nMaxLen = w / 8
    for n, t in ipairs(tAll) do
        if type(t) == "table" then
            for nu, sItem in pairs(t) do
                local ty = type(sItem)
                if not utflib.isUTFString(sItem) and ty ~= "string" and ty ~= "number" then
                    error("bad argument #" .. n .. "." .. nu .. " (string expected, got " .. ty .. ")", 3)
                end
                sItem1 = ty == "number" and tostring(sItem) or sItem
                nMaxLen = math.max(#sItem1 + 1, nMaxLen)
            end
        end
    end
    local nCols = math.floor(w / nMaxLen)
    local nLines = 0
    local function newLine()
        if bPaged and nLines >= h - 3 then
            pagedPrint()
        else
            print()
        end
        nLines = nLines + 1
    end

    local function drawCols(_t)
        local nCol = 1
        for _, s in ipairs(_t) do
            if nCol > nCols then
                nCol = 1
                newLine()
            end

            local cx, cy = term.getCursorPos()
            cx = 1 + (nCol - 1) * nMaxLen
            term.setCursorPos(cx, cy)
            uterm.write(s, term)

            nCol = nCol + 1
        end
        print()
    end

    local previous_colour = term.getTextColour()
    for _, t in ipairs(tAll) do
        if type(t) == "table" then
            if #t > 0 then
                drawCols(t)
            end
        elseif type(t) == "number" then
            term.setTextColor(t)
        end
    end
    term.setTextColor(previous_colour)
end

--[[- Prints tables in a structured form.

This accepts multiple arguments, either a table or a number. When
encountering a table, this will be treated as a table row, with each column
width being auto-adjusted.

When encountering a number, this sets the text color of the subsequent rows to it.

@tparam {string...}|number ... The rows and text colors to display.
@since 1.3
@usage

    textutils.tabulate(
      colors.orange, { "1", "2", "3" },
      colors.lightBlue, { "A", "B", "C" }
    )
]]
function utextutils.tabulate(...)
    return tabulateCommon(false, ...)
end

--[[- Prints tables in a structured form, stopping and prompting for input should
the result not fit on the terminal.

This functions identically to @{textutils.tabulate}, but will prompt for user
input should the whole output not fit on the display.

@tparam {string...}|number ... The rows and text colors to display.
@see textutils.tabulate
@see textutils.pagedPrint
@since 1.3

@usage Generates a long table, tabulates it, and prints it to the screen.

    local rows = {}
    for i = 1, 30 do rows[i] = {("Row #%d"):format(i), math.random(1, 400)} end

    textutils.pagedTabulate(colors.orange, {"Column", "Value"}, colors.lightBlue, table.unpack(rows))
]]
function utextutils.pagedTabulate(...)
    return tabulateCommon(true, ...)
end

--- Replaces certain characters in a string to make it safe for use in URLs or POST data.
--
-- @tparam string str The string to encode
-- @treturn string The encoded string.
-- @usage print("https://example.com/?view=" .. textutils.urlEncode("some text&things"))
-- @since 1.31
function utextutils.urlEncode(str)
    if not utflib.isUTFString(str) then
        expect(1, str, "string")
    end
    if str then
        str = utflib.UTFString(str)
        str = str:gsub("\n", "\r\n")
        str = str:gsub("([^A-Za-z0-9 %-%_%.])", function(c)
            local n = string.byte(c)
            return string.format("%%%02X", n)
        end)
        str = str:gsub(" ", "+")
    end
    return str
end

return utextutils
