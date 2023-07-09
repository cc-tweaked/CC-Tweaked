local expect = (require and require("cc.expect") or dofile("rom/modules/main/cc/expect.lua")).expect
local utflib = (require and require("cc.utflib") or dofile("rom/modules/main/cc/utflib.lua"))

local function twrite(sText)
    if not utflib.isUTFString(sText) then
        expect(1, sText, "UTFString") -- type(sText) == "UTFString" should never happened. Just use for triggering errors
    end
    term._writeutf8(tostring(sText))
end

local function writeutf8(sText, targetTerm)
    if not utflib.isUTFString(sText) then
        expect(1, sText, "string", "number")
    end
    expect(2, targetTerm, "table", "nil")
    targetTerm = targetTerm or term

    local w, h = targetTerm.getSize()
    local x, y = targetTerm.getCursorPos()

    local nLinesPrinted = 0
    local function newLine()
        if y + 1 <= h then
            targetTerm.setCursorPos(1, y + 1)
        else
            targetTerm.setCursorPos(1, h)
            targetTerm.scroll(1)
        end
        x, y = targetTerm.getCursorPos()
        nLinesPrinted = nLinesPrinted + 1
    end

    -- Print the line with proper word wrapping
    if not utflib.isUTFString(sText) then
        sText = utflib.UTFString(tostring(sText))
    end
    while #sText > 0 do
        local whitespace = sText:match("^[ \t]+")
        if whitespace then
            -- Print whitespace
            targetTerm.write(whitespace)
            x, y = term.getCursorPos()
            sText = sText:sub(#whitespace + 1)
        end

        local newline = sText:match("^\n")
        if newline then
            -- Print newlines
            newLine()
            sText = sText:sub(2)
        end

        local text = sText:match("^[^ \t\n]+")
        if text then
            sText = sText:sub(#text + 1)
            if #text > w then
                -- Print a multiline word
                while #text > 0 do
                    if x > w then
                        newLine()
                    end
                    targetTerm._writeutf8(tostring(text))
                    text = text:sub(w - x + 2)
                    x, y = term.getCursorPos()
                end
            else
                -- Print a word normally
                if x + #text - 1 > w then
                    newLine()
                end
                targetTerm._writeutf8(tostring(text))
                x, y = term.getCursorPos()
            end
        end
    end

    return nLinesPrinted
end

local function printutf8(...)
    local nLinesPrinted = 0
    local nLimit = select("#", ...)
    for n = 1, nLimit do
        local s = select(n, ...)
        if not utflib.isUTFString(s) then s = tostring(s) end
        if n < nLimit then
            s = s .. "\t"
        end
        nLinesPrinted = nLinesPrinted + writeutf8(s)
    end
    nLinesPrinted = nLinesPrinted + writeutf8("\n")
    return nLinesPrinted
end

local function printErrorutf8(...)
    local oldColour
    if term.isColour() then
        oldColour = term.getTextColour()
        term.setTextColour(colors.red)
    end
    printutf8(...)
    if term.isColour() then
        term.setTextColour(oldColour)
    end
end

local function readutf8(_sReplaceChar, _tHistory, _fnComplete, _sDefault)
    if not utflib.isUTFString(_sReplaceChar) then expect(1, _sReplaceChar, "string", "nil") end
    expect(2, _tHistory, "table", "nil")
    expect(3, _fnComplete, "function", "nil")
    if not utflib.isUTFString(_sDefault) then expect(4, _sDefault, "string", "nil") end

    term.setCursorBlink(true)

    local sLine
    if type(_sDefault) ~= "nil" then
        sLine = _sDefault
    else
        sLine = ""
    end
    sLine = utflib.UTFString(sLine)
    local nHistoryPos
    local nPos, nScroll = #sLine, 0
    if _sReplaceChar then
        _sReplaceChar = _sReplaceChar:sub(1, 1)
    end

    local tCompletions
    local nCompletion
    local function recomplete()
        if _fnComplete and nPos == #sLine then
            tCompletions = _fnComplete(tostring(sLine))
            if tCompletions and #tCompletions > 0 then
                nCompletion = 1
            else
                nCompletion = nil
            end
        else
            tCompletions = nil
            nCompletion = nil
        end
    end

    local function uncomplete()
        tCompletions = nil
        nCompletion = nil
    end

    local w = term.getSize()
    local sx = term.getCursorPos()

    local function redraw(_bClear)
        local cursor_pos = nPos - nScroll
        if sx + cursor_pos >= w then
            -- We've moved beyond the RHS, ensure we're on the edge.
            nScroll = sx + nPos - w
        elseif cursor_pos < 0 then
            -- We've moved beyond the LHS, ensure we're on the edge.
            nScroll = nPos
        end

        local _, cy = term.getCursorPos()
        term.setCursorPos(sx, cy)
        local sReplace = _bClear and " " or _sReplaceChar
        if sReplace then
            term._writeutf8(sReplace:rep(math.max(#sLine - nScroll, 0)))
        else
            term._writeutf8(sLine:sub(nScroll + 1))
        end

        if nCompletion then
            local sCompletion = tCompletions[nCompletion]
            local oldText, oldBg
            if not _bClear then
                oldText = term.getTextColor()
                oldBg = term.getBackgroundColor()
                term.setTextColor(colors.white)
                term.setBackgroundColor(colors.gray)
            end
            if sReplace then
                term._writeutf8(sReplace:rep(#sCompletion))
            else
                term._writeutf8(sCompletion)
            end
            if not _bClear then
                term.setTextColor(oldText)
                term.setBackgroundColor(oldBg)
            end
        end

        term.setCursorPos(sx + nPos - nScroll, cy)
    end

    local function clear()
        redraw(true)
    end

    recomplete()
    redraw()

    local function acceptCompletion()
        if nCompletion then
            -- Clear
            clear()

            -- Find the common prefix of all the other suggestions which start with the same letter as the current one
            local sCompletion = tCompletions[nCompletion]
            sLine = sLine .. sCompletion
            nPos = #sLine

            -- Redraw
            recomplete()
            redraw()
        end
    end
    while true do
        local sEvent, param, param1, param2 = os.pullEvent()
        if sEvent == "charutf" then
            -- Typed key
            clear()
            sLine = sLine:sub(1, nPos) .. param .. sLine:sub(nPos + 1)
            nPos = nPos + 1
            recomplete()
            redraw()

        elseif sEvent == "pasteutf" then
            -- Pasted text
            clear()
            param = utflib.UTFString(param)
            sLine = sLine:sub(1, nPos) .. param .. sLine:sub(nPos + 1)
            nPos = nPos + #param
            recomplete()
            redraw()

        elseif sEvent == "key" then
            if param == keys.enter or param == keys.numPadEnter then
                -- Enter/Numpad Enter
                if nCompletion then
                    clear()
                    uncomplete()
                    redraw()
                end
                break

            elseif param == keys.left then
                -- Left
                if nPos > 0 then
                    clear()
                    nPos = nPos - 1
                    recomplete()
                    redraw()
                end

            elseif param == keys.right then
                -- Right
                if nPos < #sLine then
                    -- Move right
                    clear()
                    nPos = nPos + 1
                    recomplete()
                    redraw()
                else
                    -- Accept autocomplete
                    acceptCompletion()
                end

            elseif param == keys.up or param == keys.down then
                -- Up or down
                if nCompletion then
                    -- Cycle completions
                    clear()
                    if param == keys.up then
                        nCompletion = nCompletion - 1
                        if nCompletion < 1 then
                            nCompletion = #tCompletions
                        end
                    elseif param == keys.down then
                        nCompletion = nCompletion + 1
                        if nCompletion > #tCompletions then
                            nCompletion = 1
                        end
                    end
                    redraw()

                elseif _tHistory then
                    -- Cycle history
                    clear()
                    if param == keys.up then
                        -- Up
                        if nHistoryPos == nil then
                            if #_tHistory > 0 then
                                nHistoryPos = #_tHistory
                            end
                        elseif nHistoryPos > 1 then
                            nHistoryPos = nHistoryPos - 1
                        end
                    else
                        -- Down
                        if nHistoryPos == #_tHistory then
                            nHistoryPos = nil
                        elseif nHistoryPos ~= nil then
                            nHistoryPos = nHistoryPos + 1
                        end
                    end
                    if nHistoryPos then
                        sLine = _tHistory[nHistoryPos]
                        nPos, nScroll = #sLine, 0
                    else
                        sLine = ""
                        nPos, nScroll = 0, 0
                    end
                    uncomplete()
                    redraw()

                end

            elseif param == keys.backspace then
                -- Backspace
                if nPos > 0 then
                    clear()
                    sLine = sLine:sub(1, nPos - 1) .. sLine:sub(nPos + 1)
                    nPos = nPos - 1
                    if nScroll > 0 then nScroll = nScroll - 1 end
                    recomplete()
                    redraw()
                end

            elseif param == keys.home then
                -- Home
                if nPos > 0 then
                    clear()
                    nPos = 0
                    recomplete()
                    redraw()
                end

            elseif param == keys.delete then
                -- Delete
                if nPos < #sLine then
                    clear()
                    sLine = sLine:sub(1, nPos) .. sLine:sub(nPos + 2)
                    recomplete()
                    redraw()
                end

            elseif param == keys["end"] then
                -- End
                if nPos < #sLine then
                    clear()
                    nPos = #sLine
                    recomplete()
                    redraw()
                end

            elseif param == keys.tab then
                -- Tab (accept autocomplete)
                acceptCompletion()

            end

        elseif sEvent == "mouse_click" or sEvent == "mouse_drag" and param == 1 then
            local _, cy = term.getCursorPos()
            if param1 >= sx and param1 <= w and param2 == cy then
                -- Ensure we don't scroll beyond the current line
                nPos = math.min(math.max(nScroll + param1 - sx, 0), #sLine)
                redraw()
            end

        elseif sEvent == "term_resize" then
            -- Terminal resized
            w = term.getSize()
            redraw()

        end
    end

    local _, cy = term.getCursorPos()
    term.setCursorBlink(false)
    term.setCursorPos(w + 1, cy)
    print()

    return sLine
end


return {
    twrite = twrite,
    write = writeutf8,
    print = printutf8,
    printError = printErrorutf8,
    read = readutf8
}
