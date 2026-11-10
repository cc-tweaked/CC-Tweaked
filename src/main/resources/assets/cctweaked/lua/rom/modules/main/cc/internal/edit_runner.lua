-- SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Launches a program, reports any errors, and prompts the user to close the
tab.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

This is used by the `edit` program to launch the running code.

@tparam string title The title of the multishell tab.
@tparam string path The path to the file.
@tparam string contents The contents of the file.

@local
]]
return function(title, path, contents)
    multishell.setTitle(multishell.getCurrent(), title)
    local current = term.current()
    local fn, err = load(contents, path, nil, _ENV)
    if fn then
        local exception = require "cc.internal.exception"
        local ok, err, co = exception.try(fn)

        term.redirect(current)
        term.setTextColor(term.isColour() and colours.yellow or colours.white)
        term.setBackgroundColor(colours.black)
        term.setCursorBlink(false)

        if not ok then
            printError(err)
            exception.report(err, co, { [path] = contents })
        end
    else
        local parser = require "cc.internal.syntax"
        if parser.parse_program(contents) then printError(err) end
    end

    local message = "Press any key to continue."
    local _, y = term.getCursorPos()
    local w, h = term.getSize()
    local wrapped = require("cc.strings").wrap(message, w)

    term.setTextColor(colours.white)
    term.setBackgroundColor(colours.black)

    local start_y = h - #wrapped + 1
    if y >= start_y then term.scroll(y - start_y + 1) end
    for i = 1, #wrapped do
        term.setCursorPos(1, start_y + i - 1)
        term.write(wrapped[i])
    end
    os.pullEvent('key')
    require "cc.internal.event".discard_char()
end
