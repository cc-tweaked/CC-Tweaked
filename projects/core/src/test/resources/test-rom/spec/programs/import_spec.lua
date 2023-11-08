-- SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local with_window = require "test_helpers".with_window

describe("The import program", function()
    local function create_file(name, contents)
        local did_read = false
        return {
            getName = function() return name end,
            read = function()
                if did_read then return end
                did_read = true
                return contents
            end,
            close = function() end,
        }
    end
    local function create_files(files) return { getFiles = function() return files end } end

    it("uploads files", function()
        fs.delete("transfer.txt")

        with_window(32, 5, function()
            local queue = {
                { "import" },
                { "file_transfer", create_files { create_file("transfer.txt", "empty file") } },
            }
            local co = coroutine.create(shell.run)
            for _, event in pairs(queue) do assert(coroutine.resume(co, table.unpack(event))) end
        end)

        local handle = fs.open("transfer.txt", "r")
        local contents = handle.readAll()
        handle.close()

        expect(contents):eq("empty file")
    end)
end)
