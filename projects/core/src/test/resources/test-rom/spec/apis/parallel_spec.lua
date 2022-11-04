describe("The parallel library", function()
    describe("parallel.waitForAny", function()
        it("validates arguments", function()
            expect.error(parallel.waitForAny, ""):eq("bad argument #1 (expected function, got string)")
            expect.error(parallel.waitForAny, function() end, 2):eq("bad argument #2 (expected function, got number)")
        end)

        it("returns immediately with no arguments", function()
            expect(parallel.waitForAny()):eq(0)
        end)

        it("runs functions in parallel", function()
            local entries = {}
            local function a()
                entries[#entries + 1] = "first"
                local s = coroutine.yield()
                entries[#entries + 1] = s
            end
            local function b()
                entries[#entries + 1] = "second"
                local s = coroutine.yield()
                entries[#entries + 1] = s
            end
            os.queueEvent("yield")
            parallel.waitForAny(a, b)
            expect(entries):same({ "first", "second", "yield" })
        end)

        it("accepts an arbitrary number of functions", function()
            local count = 0
            local fns = {}
            for i = 1, 50 do fns[i] = function()
                count = count + 1
                coroutine.yield()
            end end
            os.queueEvent("dummy")
            parallel.waitForAny(table.unpack(fns))
            expect(count):eq(50)
        end)

        it("passes errors to the caller", function()
            expect.error(parallel.waitForAny, function() error("Test error") end):str_match("Test error$")
        end)

        it("returns the number of the function that exited first", function()
            os.queueEvent("dummy")
            os.queueEvent("dummy")
            expect(parallel.waitForAny(function()
                coroutine.yield()
                coroutine.yield()
            end, function()
                coroutine.yield()
                return
            end, function()
                coroutine.yield()
                coroutine.yield()
            end)):eq(2)
        end)
    end)

    describe("parallel.waitForAll", function()
        it("validates arguments", function()
            expect.error(parallel.waitForAll, ""):eq("bad argument #1 (expected function, got string)")
            expect.error(parallel.waitForAll, function() end, 2):eq("bad argument #2 (expected function, got number)")
        end)

        it("returns immediately with no arguments", function()
            parallel.waitForAll()
        end)

        it("runs functions in parallel", function()
            local entries = {}
            local function a()
                entries[#entries + 1] = "first"
                local s = coroutine.yield()
                entries[#entries + 1] = s
            end
            local function b()
                entries[#entries + 1] = "second"
                local s = coroutine.yield()
                entries[#entries + 1] = s
            end
            os.queueEvent("yield")
            parallel.waitForAll(a, b)
            expect(entries):same({ "first", "second", "yield", "yield" })
        end)

        it("accepts an arbitrary number of functions", function()
            local count = 0
            local fns = {}
            for i = 1, 50 do fns[i] = function()
                count = count + 1
                coroutine.yield()
            end end
            os.queueEvent("dummy")
            parallel.waitForAll(table.unpack(fns))
            expect(count):eq(50)
        end)

        it("passes errors to the caller", function()
            expect.error(parallel.waitForAll, function() error("Test error") end):str_match("Test error$")
        end)

        it("completes all functions before exiting", function()
            local exitCount = 0
            os.queueEvent("dummy")
            os.queueEvent("dummy")
            parallel.waitForAll(function()
                coroutine.yield()
                coroutine.yield()
                exitCount = exitCount + 1
            end, function()
                coroutine.yield()
                exitCount = exitCount + 1
                return
            end, function()
                coroutine.yield()
                coroutine.yield()
                exitCount = exitCount + 1
            end)
            expect(exitCount):eq(3)
        end)
    end)
end)
