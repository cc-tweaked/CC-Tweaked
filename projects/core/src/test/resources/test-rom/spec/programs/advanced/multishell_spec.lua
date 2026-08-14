-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The multishell program", function()
    describe("multishell.setFocus", function()
        it("validates arguments", function()
            multishell.setFocus(multishell.getFocus())
            expect.error(multishell.setFocus, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)

    describe("multishell.getTitle", function()
        it("validates arguments", function()
            multishell.getTitle(1)
            expect.error(multishell.getTitle, nil):eq("bad argument #1 (number expected, got nil)")
        end)
    end)

    describe("multishell.setTitle", function()
        it("validates arguments", function()
            multishell.setTitle(1, multishell.getTitle(1))
            expect.error(multishell.setTitle, nil):eq("bad argument #1 (number expected, got nil)")
            expect.error(multishell.setTitle, 1, nil):eq("bad argument #2 (string expected, got nil)")
        end)
    end)

    describe("multishell.launch", function()
        it("validates arguments", function()
            expect.error(multishell.launch, nil):eq("bad argument #1 (table expected, got nil)")
            expect.error(multishell.launch, _ENV, nil):eq("bad argument #2 (string expected, got nil)")
        end)
    end)

    describe("the main loop", function()
        it("culls a process after exiting", function()
            local function find_local(co, find)
                for level = 1, 10 do
                    for var = 1, 255 do
                        local name, value = debug.getlocal(co, level, var)
                        if name == nil then break end
                        if find == name then return value end
                    end
                end

                fail("Cannot find local " .. find)
            end

            -- We create a very awkward program here which pulls an event (so we can inspect the intermediate state),
            -- then switches to a different tab before exiting, causing nCurrentProcess to change in the multishell
            -- loop.
            io.open("/test-files/a.lua", "w"):write([[os.pullEvent("mouse_click"); multishell.setFocus(1)]]):close()

            local window = window.create(term.current(), 1, 1, 51, 19, false)
            local co = coroutine.create(function() os.run({ shell = shell }, "rom/programs/advanced/multishell.lua") end)
            local function resume(...)
                local old = term.redirect(window)
                local ok, err = coroutine.resume(co, ...)
                term.redirect(old)
                if not ok then fail("Coroutine failed: " .. err) end
            end

            resume()
            local multishell = find_local(co, "multishell")
            expect(multishell.getFocus()):eq(1)
            expect(multishell.getCount()):eq(1)

            resume("paste", "fg /test-files/a.lua")
            resume("key", keys.enter, false)
            expect(multishell.getFocus()):eq(2)
            expect(multishell.getCount()):eq(2)

            resume("mouse_click", 1, 1, 2)
            expect(multishell.getFocus()):eq(1)
            expect(multishell.getCount()):eq(1)
        end)
    end)
end)
