--- Tests the io library is (mostly) consistent with PUC Lua.
--
-- These tests are based on the tests for Lua 5.1 and 5.3

describe("The io library", function()
    local file = "/test-files/tmp.txt"
    local otherfile = "/test-files/tmp2.txt"

    local t = '0123456789'
    for _ = 1, 12 do t = t .. t end
    assert(#t == 10 * 2 ^ 12)

    local function read_all(f)
        local h = fs.open(f, "rb")
        local contents = h.readAll()
        h.close()
        return contents
    end

    local function write_file(f, contents)
        local h = fs.open(f, "wb")
        h.write(contents)
        h.close()
    end

    local function setup()
        write_file(file, "\"�lo\"{a}\nsecond line\nthird line \n�fourth_line\n\n\9\9  3450\n")
    end

    describe("io.close", function()
        it("cannot close stdin", function()
            expect{ io.stdin:close() }:same { nil, "attempt to close standard stream" }
        end)
        it("cannot close stdout", function()
            expect{ io.stdout:close() }:same { nil, "attempt to close standard stream" }
        end)
        it("cannot close stdout", function()
            expect{ io.stdout:close() }:same { nil, "attempt to close standard stream" }
        end)
    end)

    it("io.input on a handle returns that handle", function()
        expect(io.input(io.stdin)):equals(io.stdin)
    end)

    it("io.output on a handle returns that handle", function()
        expect(io.output(io.stdout)):equals(io.stdout)
    end)

    it("defines a __name field", function()
        expect(getmetatable(io.input()).__name):eq("FILE*")
    end)

    describe("io.type", function()
        it("returns file on handles", function()
            local handle = io.input()
            expect(handle):type("table")
            expect(io.type(handle)):equals("file")
            expect(io.type(io.stdin)):equals("file")
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

        it("closes the file", function()
            setup()

            local n = 0
            local f = io.lines(file)
            while f() do n = n + 1 end
            expect(n):eq(6)

            expect.error(f):eq("file is already closed")
            expect.error(f):eq("file is already closed")
        end)

        it("can copy a file", function()
            setup()

            local n = 0
            io.output(otherfile)
            for l in io.lines(file) do
                io.write(l, "\n")
                n = n + 1
            end
            io.close()
            expect(n):eq(6)

            io.input(file)
            local f = io.open(otherfile):lines()
            local n = 0
            for l in io.lines() do
                expect(l):eq(f())
                n = n + 1
            end
            expect(n):eq(6)
        end)

        it("does not close on a normal file handle", function()
            setup()

            local f = assert(io.open(file))
            local n = 0
            for _ in f:lines() do n = n + 1 end
            expect(n):eq(6)

            expect(tostring(f):sub(1, 5)):eq("file ")
            assert(f:close())

            expect(tostring(f)):eq("file (closed)")
            expect(io.type(f)):eq("closed file")
        end)

        it("accepts multiple arguments", function()
            write_file(file, "0123456789\n")
            for a, b in io.lines(file, 1, 1) do
                if a == "\n" then
                    expect(b):eq(nil)
                else
                    expect(tonumber(a)):eq(tonumber(b) - 1)
                end
            end

            for a, b, c in io.lines(file, 1, 2, "a") do
                expect(a):eq("0")
                expect(b):eq("12")
                expect(c):eq("3456789\n")
            end

            for a, b, c in io.lines(file, "a", 0, 1) do
                if a == "" then break end
                expect(a):eq("0123456789\n")
                expect(b):eq(nil)
                expect(c):eq(nil)
            end

            write_file(file, "00\n10\n20\n30\n40\n")
            for a, b in io.lines(file, "n", "n") do
                if a == 40 then
                    expect(b):eq(nil)
                else
                    expect(a):eq(b - 10)
                end
            end
        end)
    end)

    describe("io.open", function()
        it("validates arguments", function()
            io.open("")
            io.open("", "r")

            expect.error(io.open, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(io.open, "", false):eq("bad argument #2 (expected string, got boolean)")
        end)

        it("checks the mode", function()
            io.open(file, "w"):close()

            -- This really should be invalid mode, but I'll live.
            expect.error(io.open, file, "rw"):str_match("Unsupported mode")
            -- TODO: expect.error(io.open, file, "rb+"):str_match("Unsupported mode")
            expect.error(io.open, file, "r+bk"):str_match("Unsupported mode")
            expect.error(io.open, file, ""):str_match("Unsupported mode")
            expect.error(io.open, file, "+"):str_match("Unsupported mode")
            expect.error(io.open, file, "b"):str_match("Unsupported mode")

            assert(io.open(file, "r+b")):close()
            assert(io.open(file, "r+")):close()
            assert(io.open(file, "rb")):close()
        end)

        it("returns an error message on non-existent files", function()
            local a, b = io.open('xuxu_nao_existe')
            expect(a):equals(nil)
            expect(b):type("string")
        end)
    end)

    describe("a readable handle", function()
        it("cannot be written to", function()
            write_file(file, "")
            io.input(file)
            expect { io.input():write("xuxu") }:same { nil, "file is not writable" }
            io.input(io.stdin)
        end)

        it("supports various modes", function()
            write_file(file, "alo\n " .. t .. " ;end of file\n")

            io.input(file)
            expect(io.read()):eq("alo")
            expect(io.read(1)):eq(' ')
            expect(io.read(#t)):eq(t)
            expect(io.read(1)):eq(' ')
            expect(io.read(0))
            expect(io.read('*a')):eq(';end of file\n')
            expect(io.read(0)):eq(nil)
            expect(io.close(io.input())):eq(true)

            fs.delete(file)
        end)

        it("support seeking", function()
            setup()
            io.input(file)

            expect(io.read(0)):eq("")   -- not eof
            expect(io.read(5, '*l')):eq('"�lo"')
            expect(io.read(0)):eq("")
            expect(io.read()):eq("second line")
            local x = io.input():seek()
            expect(io.read()):eq("third line ")
            assert(io.input():seek("set", x))
            expect(io.read('*l')):eq("third line ")
            expect(io.read(1)):eq("�")
            expect(io.read(#"fourth_line")):eq("fourth_line")
            assert(io.input():seek("cur", -#"fourth_line"))
            expect(io.read()):eq("fourth_line")
            expect(io.read()):eq("")  -- empty line
            expect(io.read(8)):eq('\9\9  3450') -- FIXME: Not actually supported
            expect(io.read(1)):eq('\n')
            expect(io.read(0)):eq(nil)  -- end of file
            expect(io.read(1)):eq(nil)  -- end of file
            expect(({ io.read(1) })[2]):eq(nil)
            expect(io.read()):eq(nil)  -- end of file
            expect(({ io.read() })[2]):eq(nil)
            expect(io.read('*n')):eq(nil)  -- end of file
            expect(({ io.read('*n') })[2]):eq(nil)
            expect(io.read('*a')):eq('')  -- end of file (OK for `*a')
            expect(io.read('*a')):eq('')  -- end of file (OK for `*a')

            io.close(io.input())
        end)

        it("supports the 'L' mode", function()
            write_file(file, "\n\nline\nother")

            io.input(file)
            expect(io.read"L"):eq("\n")
            expect(io.read"L"):eq("\n")
            expect(io.read"L"):eq("line\n")
            expect(io.read"L"):eq("other")
            expect(io.read"L"):eq(nil)
            io.input():close()

            local f = assert(io.open(file))
            local s = ""
            for l in f:lines("L") do s = s .. l end
            expect(s):eq("\n\nline\nother")
            f:close()

            io.input(file)
            s = ""
            for l in io.lines(nil, "L") do s = s .. l end
            expect(s):eq("\n\nline\nother")
            io.input():close()

            s = ""
            for l in io.lines(file, "L") do s = s .. l end
            expect(s):eq("\n\nline\nother")

            s = ""
            for l in io.lines(file, "l") do s = s .. l end
            expect(s):eq("lineother")

            write_file(file, "a = 10 + 34\na = 2*a\na = -a\n")
            local t = {}
            load(io.lines(file, "L"), nil, nil, t)()
            expect(t.a):eq(-((10 + 34) * 2))
        end)
    end)

    describe("a writable handle", function()
        it("supports seeking", function()
            fs.delete(file)
            io.output(file)

            expect(io.output()):not_equals(io.stdout)

            expect(io.output():seek()):equal(0)
            assert(io.write("alo alo"))
            expect(io.output():seek()):equal(#"alo alo")
            expect(io.output():seek("cur", -3)):equal(#"alo alo" - 3)
            assert(io.write("joao"))
            expect(io.output():seek("end")):equal(#"alo joao")

            expect(io.output():seek("set")):equal(0)

            assert(io.write('"�lo"', "{a}\n", "second line\n", "third line \n"))
            assert(io.write('�fourth_line'))

            io.output(io.stdout)
            expect(io.output()):equals(io.stdout)
        end)

        it("supports appending", function()
            io.output(file)
            io.write("alo\n")
            io.close()
            expect.error(io.write)

            local f = io.open(file, "a")
            io.output(f)

            assert(io.write(' ' .. t .. ' '))
            assert(io.write(';', 'end of file\n'))
            f:flush()
            io.flush()
            f:close()

            expect(read_all(file)):eq("alo\n " .. t .. " ;end of file\n")
        end)
    end)
end)
