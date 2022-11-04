describe("Coroutines", function()
    local function assert_resume(ok, ...)
        if ok then return table.pack(...) end
        error(..., 0)
    end

    --- Run a function in a coroutine, "echoing" the yielded value back as the resumption value.
    local function coroutine_echo(f)
        local co = coroutine.create(f)
        local result = { n = 0 }
        while coroutine.status(co) ~= "dead" do
            result = assert_resume(coroutine.resume(co, table.unpack(result, 1, result.n)))
        end

        return table.unpack(result, 1, result.n)
    end

    describe("allow yielding", function()
        --[[
        Tests for some non-standard yield locations. I'm not saying that users
        /should/ use this, but it's useful for us to allow it in order to suspend the
        VM in arbitrary locations.

        Cobalt does support this within load too, but that's unlikely to be supported
        in the future.

        These tests were split over about 7 files in Cobalt and are in one massive one
        in this test suite. Sorry.
        ]]

        it("within debug hooks", function()
            coroutine_echo(function()
                local counts = { call = 0, ['return'] = 0, count = 0, line = 0 }

                debug.sethook(function(kind)
                    counts[kind] = (counts[kind] or 0) + 1
                    expect(coroutine.yield(kind)):eq(kind)
                end, "crl", 1)

                expect(string.gsub("xyz", "x", "z")):eq("zyz")
                expect(pcall(function()
                    local x = 0
                    for i = 1, 5 do x = x + i end
                end)):eq(true)

                debug.sethook(nil)

                -- These numbers are going to vary beyond the different VMs a
                -- little. As long as they're non-0, it's all fine.
                expect(counts.call):ne(0)
                expect(counts['return']):ne(0)
                expect(counts.count):ne(0)
                expect(counts.line):ne(0)
            end)
        end)

        it("within string.gsub", function()
            local result, count = coroutine_echo(function()
                return ("hello world"):gsub("%w", function(entry)
                    local x = coroutine.yield(entry)
                    return x:upper()
                end)
            end)

            expect(result):eq("HELLO WORLD")
            expect(count):eq(10)
        end)

        describe("within pcall", function()
            it("with no error", function()
                local ok, a, b, c = coroutine_echo(function()
                    return pcall(function()
                        local a, b, c = coroutine.yield(1, 2, 3)
                        return a, b, c
                    end)
                end)

                expect(ok):eq(true)
                expect({ a, b, c }):same { 1, 2, 3 }
            end)

            it("with an error", function()
                local ok, msg = coroutine_echo(function()
                    return pcall(function()
                        local a, b, c = coroutine.yield(1, 2, 3)
                        expect({ a, b, c }):same { 1, 2, 3 }
                        error("Error message", 0)
                    end)
                end)

                expect(ok):eq(false)
                expect(msg):eq("Error message")
            end)
        end)

        it("within table.foreach", function()
            coroutine_echo(function()
                local x = { 3, "foo", 4, 1 }
                local idx = 1
                table.foreach(x, function(key, val)
                    expect(key):eq(idx)
                    expect(val):eq(x[idx])
                    expect(coroutine.yield(val)):eq(val)

                    idx = idx + 1
                end)
            end)
        end)

        it("within table.foreachi", function()
            coroutine_echo(function()
                local x = { 3, "foo", 4, 1 }
                local idx = 1
                table.foreachi(x, function(key, val)
                    expect(key):eq(idx)
                    expect(val):eq(x[idx])
                    expect(coroutine.yield(val)):eq(val)

                    idx = idx + 1
                end)
            end)
        end)

        describe("within table.sort", function()
            it("with a yielding comparator", function()
                coroutine_echo(function()
                    local x = { 32, 2, 4, 13 }
                    table.sort(x, function(a, b)
                        local x, y = coroutine.yield(a, b)
                        expect(x):eq(a)
                        expect(y):eq(b)

                        return a < b
                    end)

                    expect(x[1]):eq(2)
                    expect(x[2]):eq(4)
                    expect(x[3]):eq(13)
                    expect(x[4]):eq(32)
                end)
            end)

            it("within a yielding metatable comparator", function()
                local meta = {
                    __lt = function(a, b)
                        local x, y = coroutine.yield(a, b)
                        expect(x):eq(a)
                        expect(y):eq(b)

                        return a.x < b.x
                    end,
                }

                local function create(val) return setmetatable({ x = val }, meta) end

                coroutine_echo(function()
                    local x = { create(32), create(2), create(4), create(13) }
                    table.sort(x)

                    expect(x[1].x):eq(2)
                    expect(x[2].x):eq(4)
                    expect(x[3].x):eq(13)
                    expect(x[4].x):eq(32)
                end)
            end)
        end)

        describe("within xpcall", function()
            it("within the main function", function()
                -- Ensure that yielding within a xpcall works as expected
                coroutine_echo(function()
                    local ok, a, b, c = xpcall(function()
                        return coroutine.yield(1, 2, 3)
                    end, function(msg) return msg .. "!" end)

                    expect(true):eq(ok)
                    expect(1):eq(a)
                    expect(2):eq(b)
                    expect(3):eq(c)
                end)
            end)

            it("within the main function (with an error)", function()
                coroutine_echo(function()
                    local ok, msg = xpcall(function()
                        local a, b, c = coroutine.yield(1, 2, 3)
                        expect(1):eq(a)
                        expect(2):eq(b)
                        expect(3):eq(c)

                        error("Error message", 0)
                    end, function(msg) return msg .. "!" end)

                    expect(false):eq(ok)
                    expect("Error message!"):eq(msg)
                end)
            end)

            it("with an error in the error handler", function()
                coroutine_echo(function()
                    local ok, msg = xpcall(function()
                        local a, b, c = coroutine.yield(1, 2, 3)
                        expect(1):eq(a)
                        expect(2):eq(b)
                        expect(3):eq(c)

                        error("Error message")
                    end, function(msg) error(msg) end)

                    expect(false):eq(ok)
                    expect("error in error handling"):eq(msg)
                end)
            end)

            it("within the error handler", function()
                coroutine_echo(function()
                    local ok, msg = xpcall(function()
                        local a, b, c = coroutine.yield(1, 2, 3)
                        expect(1):eq(a)
                        expect(2):eq(b)
                        expect(3):eq(c)

                        error("Error message", 0)
                    end, function(msg)
                        return coroutine.yield(msg) .. "!"
                    end)

                    expect(false):eq(ok)
                    expect("Error message!"):eq(msg)
                end)
            end)

            it("within the error handler with an error", function()
                coroutine_echo(function()
                    local ok, msg = xpcall(function()
                        local a, b, c = coroutine.yield(1, 2, 3)
                        expect(1):eq(a)
                        expect(2):eq(b)
                        expect(3):eq(c)

                        error("Error message", 0)
                    end, function(msg)
                        coroutine.yield(msg)
                        error("nope")
                    end)

                    expect(false):eq(ok)
                    expect("error in error handling"):eq(msg)
                end)
            end)
        end)

        it("within metamethods", function()
            local create, ops
            create = function(val) return setmetatable({ x = val }, ops) end
            ops = {
                __add = function(x, y)
                    local a, b = coroutine.yield(x, y)
                    return create(a.x + b.x)
                end,
                __div = function(x, y)
                    local a, b = coroutine.yield(x, y)
                    return create(a.x / b.x)
                end,
                __concat = function(x, y)
                    local a, b = coroutine.yield(x, y)
                    return create(a.x .. b.x)
                end,
                __eq = function(x, y)
                    local a, b = coroutine.yield(x, y)
                    return a.x == b.x
                end,
                __lt = function(x, y)
                    local a, b = coroutine.yield(x, y)
                    return a.x < b.x
                end,
                __index = function(tbl, key)
                    local res = coroutine.yield(key)
                    return res:upper()
                end,
                __newindex = function(tbl, key, val)
                    local rKey, rVal = coroutine.yield(key, val)
                    rawset(tbl, rKey, rVal .. "!")
                end,
            }

            local varA = create(2)
            local varB = create(3)

            coroutine_echo(function()
                expect(5):eq((varA + varB).x)
                expect(5):eq((varB + varA).x)
                expect(4):eq((varA + varA).x)
                expect(6):eq((varB + varB).x)

                expect(2 / 3):eq((varA / varB).x)
                expect(3 / 2):eq((varB / varA).x)
                expect(1):eq((varA / varA).x)
                expect(1):eq((varB / varB).x)

                expect("23"):eq((varA .. varB).x)
                expect("32"):eq((varB .. varA).x)
                expect("22"):eq((varA .. varA).x)
                expect("33"):eq((varB .. varB).x)
                expect("33333"):eq((varB .. varB .. varB .. varB .. varB).x)

                expect(false):eq(varA == varB)
                expect(false):eq(varB == varA)
                expect(true):eq(varA == varA)
                expect(true):eq(varB == varB)

                expect(true):eq(varA < varB)
                expect(false):eq(varB < varA)
                expect(false):eq(varA < varA)
                expect(false):eq(varB < varB)

                expect(true):eq(varA <= varB)
                expect(false):eq(varB <= varA)
                expect(true):eq(varA <= varA)
                expect(true):eq(varB <= varB)

                expect("HELLO"):eq(varA.hello)
                varA.hello = "bar"
                expect("bar!"):eq(varA.hello)
            end)


        end)
    end)
end)
