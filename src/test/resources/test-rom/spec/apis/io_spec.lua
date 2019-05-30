--- Tests the io library is (mostly) consistent with PUC Lua.
--
-- These tests are based on the tests for Lua 5.1

describe("The io library", function()
    it("io.input on a handle returns that handle", function()
        expect(io.input(io.stdin)):equals(io.stdin)
    end)

    it("io.output on a handle returns that handle", function()
        expect(io.output(io.stdout)):equals(io.stdout)
    end)

    describe("io.type", function()
        it("returns file on handles", function()
            local handle = io.input()
            expect(handle):type("table")
            expect(io.type(handle)):equals("file")
        end)

        it("returns nil on values", function()
            expect(io.type(8)):equals(nil)
        end)

        it("returns nil on tables", function()
            expect(io.type(setmetatable({}, {}))):equals(nil)
        end)
    end)

    describe("io.lines", function()
        it("validates arguments", function()
            io.lines(nil)
            expect.error(io.lines, ""):eq("/: No such file")
            expect.error(io.lines, false):eq("bad argument #1 (expected string, got boolean)")
        end)
    end)

    describe("io.open", function()
        it("validates arguments", function()
            io.open("")
            io.open("", "r")

            expect.error(io.open, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(io.open, "", false):eq("bad argument #2 (expected string, got boolean)")
        end)

        it("returns an error message on non-existent files", function()
            local a, b = io.open('xuxu_nao_existe')
            expect(a):equals(nil)
            expect(b):type("string")
        end)
    end)

    pending("io.output allows redirecting and seeking", function()
        fs.delete("/tmp/io_spec.txt")

        io.output("/tmp/io_spec.txt")

        expect(io.output()):not_equals(io.stdout)

        expect(io.output():seek()):equal(0)
        assert(io.write("alo alo"))
        expect(io.output():seek()):equal(#("alo alo"))
        expect(io.output():seek("cur", -3)):equal(#("alo alo") - 3)
        assert(io.write("joao"))
        expect(io.output():seek("end"):equal(#("alo joao")))

        expect(io.output():seek("set")):equal(0)

        assert(io.write('"�lo"', "{a}\n", "second line\n", "third line \n"))
        assert(io.write('�fourth_line'))

        io.output(io.stdout)
        expect(io.output()):equals(io.stdout)
    end)
end)
