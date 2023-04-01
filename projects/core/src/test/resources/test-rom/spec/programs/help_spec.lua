-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program
local with_window_lines = require "test_helpers".with_window_lines

describe("The help program", function()
    local function stub_help(content)
        local name = "/help_file.txt"
        io.open(name, "wb"):write(content):close()
        stub(help, "lookup", function() return name end)
    end

    local function capture_help(width, height, content)
        stub_help(content)

        local co = coroutine.create(shell.run)
        local window = with_window_lines(width, height, function()
            local ok, err = coroutine.resume(co, "help topic")
            if not ok then error(err, 0) end
        end)
        return coroutine.status(co) == "dead", window
    end

    it("errors when there is no such help file", function()
        expect(capture("help nothing"))
            :matches { ok = true, error = "No help available\n", output = "" }
    end)

    it("prints a short file directly", function()
        local dead, output = capture_help(10, 3, "a short\nfile")
        expect(dead):eq(true)
        expect(output):same {
            "a short   ",
            "file      ",
            "          ",
        }
    end)

    it("launches the viewer for a longer file", function()
        local dead, output = capture_help(10, 3, "a longer\nfile\nwith content")
        expect(dead):eq(false)
        expect(output):same {
            "a longer  ",
            "file      ",
            "Help: topi",
        }
    end)
end)
