local with_window = require "test_helpers".with_window

describe("cc.pretty", function()
    local pp = require("cc.pretty")

    describe("text", function()
        it("is constant for the empty string", function()
            expect(pp.text("")):eq(pp.empty)
        end)

        it("is constant for a space", function()
            expect(pp.text(" ")):eq(pp.space)
        end)

        it("is constant for a newline", function()
            expect(pp.text("\n")):eq(pp.space_line)
        end)

        it("validates arguments", function()
            expect.error(pp.text, 123):eq("bad argument #1 (expected string, got number)")
            expect.error(pp.text, "", ""):eq("bad argument #2 (expected number, got string)")
        end)

        it("produces text documents", function()
            expect(pp.text("a")):same({ tag = "text", text = "a" })
            expect(pp.text("a", colours.grey)):same({ tag = "text", text = "a", colour = colours.grey })
        end)

        it("splits lines", function()
            expect(pp.text("a\nb"))
                :same(pp.concat(pp.text("a"), pp.space_line, pp.text("b")))
            expect(pp.text("ab\ncd\nef"))
                :same(pp.concat(pp.text("ab"), pp.space_line, pp.text("cd"), pp.space_line, pp.text("ef")))
        end)

        it("preserves empty lines", function()
            expect(pp.text("a\n\nb"))
                :same(pp.concat(pp.text("a"), pp.space_line, pp.space_line, pp.text("b")))
            expect(pp.text("\n\nb"))
                :same(pp.concat(pp.space_line, pp.space_line, pp.text("b")))
            expect(pp.text("a\n\n"))
                :same(pp.concat(pp.text("a"), pp.space_line, pp.space_line))
        end)
    end)

    describe("concat", function()
        it("returns empty with 0 arguments", function()
            expect(pp.concat()):eq(pp.empty)
        end)

        it("acts as the identity with 1 argument", function()
            local x = pp.text("test")
            expect(pp.concat(x)):eq(x)
        end)

        it("coerces strings", function()
            expect(pp.concat("a", "b")):same(pp.concat(pp.text("a"), pp.text("b")))
        end)

        it("validates arguments", function()
            expect.error(pp.concat, 123):eq("bad argument #1 (expected document, got number)")
            expect.error(pp.concat, "", {}):eq("bad argument #2 (expected document, got table)")
        end)

        it("can be used as an operator", function()
            local a, b = pp.text("a"), pp.text("b")
            expect(pp.concat(a, b)):same(a .. b)
        end)
    end)

    describe("group", function()
        it("is idempotent", function()
            local x = pp.group(pp.text("a\nb"))
            expect(pp.group(x)):eq(x)
        end)

        it("does nothing for flat strings", function()
            local x = pp.text("a")
            expect(pp.group(x)):eq(x)
        end)
    end)

    -- Allows us to test
    local function test_output(display)
        it("displays the empty document", function()
            expect(display(pp.empty)):same { "" }
        end)

        it("displays a multiline string", function()
            expect(display(pp.text("hello\nworld"))):same {
                "hello",
                "world",
            }
        end)

        it("displays a nested string", function()
            expect(display(pp.nest(2, pp.concat("hello", pp.line, "world")))):same {
                "hello",
                "  world",
            }
        end)

        it("displays a flattened group", function()
            expect(display(pp.group(pp.concat("hello", pp.space_line, "world")))):same {
                "hello world",
            }

            expect(display(pp.group(pp.concat("hello", pp.line, "world")))):same {
                "helloworld",
            }
        end)

        it("displays an expanded group", function()
            expect(display(pp.group(pp.concat("hello darkness", pp.space_line, "my old friend")))):same {
                "hello darkness",
                "my old friend",
            }
        end)

        it("group removes nest", function()
            expect(display(pp.group(pp.nest(2, pp.concat("hello", pp.space_line, "world"))))):same {
                "hello world",
            }
        end)
    end

    describe("write", function()
        local function display(doc)
            local w = with_window(20, 10, function() pp.write(doc) end)
            local _, y = w.getCursorPos()

            local out = {}
            for i = 1, y do out[i] = w.getLine(i):gsub("%s+$", "") end
            return out
        end

        test_output(display)

        it("wraps a long string", function()
            expect(display(pp.text("hello world this is a long string which will wrap"))):same {
                "hello world this is",
                "a long string which",
                "will wrap",
            }
        end)
    end)

    describe("render", function()
        local function display(doc)
            local rendered = pp.render(doc, 20)
            local n, lines = 1, {}
            for line in (rendered .. "\n"):gmatch("([^\n]*)\n") do lines[n], n = line, n + 1 end
            return lines
        end

        test_output(display)

        it("does not wrap a long string", function()
            expect(display(pp.text("hello world this is a long string which will wrap"))):same {
                "hello world this is a long string which will wrap",
            }
        end)
    end)

    describe("pretty", function()
        -- We make use of "render" here, as it's considerably easier than checking against the actual structure.
        -- However, it does also mean our tests are less unit-like.
        local function pretty(x, width, options) return pp.render(pp.pretty(x, options), width) end

        describe("tables", function()
            it("displays empty tables", function()
                expect(pp.pretty({})):same(pp.text("{}"))
            end)

            it("displays list-like tables", function()
                expect(pretty({ 1, 2, 3 })):eq("{ 1, 2, 3 }")
            end)

            it("displays mixed tables", function()
                expect(pretty({ n = 3, 1, 2, 3 })):eq("{ 1, 2, 3, n = 3 }")
            end)

            it("escapes keys", function()
                expect(pretty({ ["and"] = 1, ["not that"] = 2 })):eq('{ ["and"] = 1, ["not that"] = 2 }')
            end)

            it("sorts keys", function()
                expect(pretty({ c = 1, b = 2, a = 3 })):eq('{ a = 3, b = 2, c = 1 }')
            end)

            it("groups tables", function()
                expect(pretty({ 1, 2, 3 }, 4)):eq("{\n  1,\n  2,\n  3\n}")
            end)

            it("handles sparse tables", function()
                local tbl = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }
                tbl[4] = nil

                expect(tostring(pp.pretty(tbl))):eq("{ 1, 2, 3, nil, 5, 6, 7, 8, 9, 10 }")
            end)
        end)

        it("shows numbers", function()
            expect(pretty(123)):eq("123")
        end)

        it("shows strings", function()
            expect(pretty("hello\nworld")):eq('"hello\\nworld"')
        end)

        describe("functions", function()
            it("shows functions", function()
                expect(pretty(pretty)):eq(tostring(pretty))
            end)

            it("shows function arguments", function()
                local f = function(a, ...) end
                expect(pretty(f, nil, { function_args = true })):eq(tostring(f) .. "(a, ...)")
            end)

            it("shows the function source", function()
                local f = function(a, ...) end
                expect(pretty(f, nil, { function_source = true }))
                    :str_match("^function<.*pretty_spec%.lua:%d+>$")
            end)
        end)
    end)
end)
