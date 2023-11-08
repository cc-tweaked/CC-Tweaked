-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The fs library", function()
    local test_root = "/test-files/fs"
    local function test_file(path) return fs.combine(test_root, path) end
    before_each(function() fs.delete(test_root) end)

    local function create_test_file(contents)
        local path = test_file(("test_%04x.txt"):format(math.random(2 ^ 16)))

        local handle = fs.open(path, "wb")
        handle.write(contents)
        handle.close()

        return path
    end

    describe("fs.complete", function()
        it("validates arguments", function()
            fs.complete("", "")
            fs.complete("", "", true)
            fs.complete("", "", nil, true)

            expect.error(fs.complete, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(fs.complete, "", nil):eq("bad argument #2 (string expected, got nil)")
            expect.error(fs.complete, "", "", 1):eq("bad argument #3 (boolean expected, got number)")
            expect.error(fs.complete, "", "", true, 1):eq("bad argument #4 (boolean expected, got number)")
        end)

        describe("include_hidden", function()
            local dir = "tmp/hidden"
            local function setup_tree()
                fs.delete(dir)
                fs.makeDir(dir)
                fs.open(dir .. "/file.txt", "w").close()
                fs.open(dir .. "/.hidden.txt", "w").close()
            end

            it("hides hidden files", function()
                setup_tree()
                local opts = { include_files = true, include_dirs = false, include_hidden = false }

                expect(fs.complete("", dir, opts)):same { "../", "file.txt" }
                expect(fs.complete(dir .. "/", "", opts)):same { "file.txt" }
            end)

            it("shows hidden files when typing a dot", function()
                setup_tree()
                local opts = { include_files = true, include_dirs = false, include_hidden = false }

                expect(fs.complete(".", dir, opts)):same { "./", "hidden.txt" }
                expect(fs.complete(dir .. "/.", "", opts)):same { "hidden.txt" }

                -- Also test
                expect(fs.complete(dir .. "/file", "", opts)):same { ".txt" }
                expect(fs.complete(dir .. "/file.", "", opts)):same { "txt" }
                expect(fs.complete("file", dir, opts)):same { ".txt" }
                expect(fs.complete("file.", dir, opts)):same { "txt" }
            end)

            it("shows hidden files when include_hidden is true", function()
                setup_tree()
                local opts = { include_files = true, include_dirs = false, include_hidden = true }

                expect(fs.complete("", dir, opts)):same { "../", ".hidden.txt", "file.txt" }
                expect(fs.complete(dir .. "/", "", opts)):same { ".hidden.txt", "file.txt" }
            end)
        end)
    end)

    describe("fs.isDriveRoot", function()
        it("validates arguments", function()
            fs.isDriveRoot("")

            expect.error(fs.isDriveRoot, nil):eq("bad argument #1 (string expected, got nil)")
        end)

        it("correctly identifies drive roots", function()
            expect(fs.isDriveRoot("/rom")):eq(true)
            expect(fs.isDriveRoot("/")):eq(true)
            expect(fs.isDriveRoot("/rom/startup.lua")):eq(false)
            expect(fs.isDriveRoot("/rom/programs/delete.lua")):eq(false)
        end)
    end)

    describe("fs.list", function()
        it("fails on files", function()
            expect.error(fs.list, "rom/startup.lua"):eq("/rom/startup.lua: Not a directory")
            expect.error(fs.list, "startup.lua"):eq("/startup.lua: Not a directory")
        end)

        it("fails on non-existent nodes", function()
            expect.error(fs.list, "rom/x"):eq("/rom/x: No such file")
            expect.error(fs.list, "x"):eq("/x: No such file")
        end)
    end)

    describe("fs.find", function()
        it("fails on invalid paths", function()
            expect.error(fs.find, ".."):eq("/..: Invalid Path")
            expect.error(fs.find, "../foo/bar"):eq("/../foo/bar: Invalid Path")
        end)

        it("returns nothing on non-existent files", function()
            expect(fs.find("no/such/file")):same {}
            expect(fs.find("no/such/*")):same {}
            expect(fs.find("no/*/file")):same {}
        end)

        it("returns a single file", function()
            expect(fs.find("rom")):same { "rom" }
            expect(fs.find("rom/motd.txt")):same { "rom/motd.txt" }
        end)

        it("supports the '*' wildcard", function()
            expect(fs.find("rom/*")):same {
                "rom/apis",
                "rom/autorun",
                "rom/help",
                "rom/modules",
                "rom/motd.txt",
                "rom/programs",
                "rom/startup.lua",
            }
            expect(fs.find("rom/*/command")):same {
                "rom/apis/command",
                "rom/modules/command",
                "rom/programs/command",
            }

            expect(fs.find("rom/*/lua*")):same {
                "rom/help/lua.txt",
                "rom/programs/lua.lua",
            }
        end)

        it("supports the '?' wildcard", function()
            expect(fs.find("rom/programs/mo??.lua")):same {
                "rom/programs/motd.lua",
                "rom/programs/move.lua",
            }
        end)
    end)

    describe("fs.combine", function()
        it("removes . and ..", function()
            expect(fs.combine("./a/b")):eq("a/b")
            expect(fs.combine("a/b", "../c")):eq("a/c")
            expect(fs.combine("a", "../c")):eq("c")
            expect(fs.combine("a", "../../c")):eq("../c")
        end)

        it("combines empty paths", function()
            expect(fs.combine("a")):eq("a")
            expect(fs.combine("a", "")):eq("a")
            expect(fs.combine("", "a")):eq("a")
            expect(fs.combine("a", "", "b", "c")):eq("a/b/c")
        end)
    end)

    describe("fs.getSize", function()
        it("fails on non-existent nodes", function()
            expect.error(fs.getSize, "rom/x"):eq("/rom/x: No such file")
            expect.error(fs.getSize, "x"):eq("/x: No such file")
        end)
    end)

    describe("fs.open", function()
        local function read_tests(mode)
            it("errors when closing twice", function()
                local handle = fs.open("rom/startup.lua", "rb")
                handle.close()
                expect.error(handle.close):eq("attempt to use a closed file")
            end)

            it("reads multiple bytes", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, mode)
                expect(handle.read(3)):eq("an ")
                handle.close()
            end)

            it("errors reading a negative number of bytes", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, mode)
                expect(handle.read(0)):eq("")
                expect.error(handle.read, -1):str_match("^Cannot read a negative number of [a-z]+$")
                handle.close()
            end)

            it("reads multiple bytes longer than the file", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, mode)
                expect(handle.read(100)):eq("an example file")
                handle.close()
            end)

            it("can read a line of text", function()
                local file = create_test_file "some\nfile\r\ncontents\n\n"

                local handle = fs.open(file, mode)
                expect(handle.readLine()):eq("some")
                expect(handle.readLine()):eq("file")
                expect(handle.readLine()):eq("contents")
                expect(handle.readLine()):eq("")
                expect(handle.readLine()):eq(nil)
                handle.close()
            end)

            it("can read a line of text with the trailing separator", function()
                local file = create_test_file "some\nfile\r\ncontents\r!\n\n"

                local handle = fs.open(file, mode)
                expect(handle.readLine(true)):eq("some\n")
                expect(handle.readLine(true)):eq("file\r\n")
                expect(handle.readLine(true)):eq("contents\r!\n")
                expect(handle.readLine(true)):eq("\n")
                expect(handle.readLine(true)):eq(nil)
                handle.close()
            end)
        end

        describe("reading", function()
            it("fails on directories", function()
                expect { fs.open("rom", "r") }:same { nil, "/rom: Not a file" }
                expect { fs.open("", "r") }:same { nil, "/: Not a file" }
            end)

            it("fails on non-existent nodes", function()
                expect { fs.open("rom/x", "r") }:same { nil, "/rom/x: No such file" }
                expect { fs.open("x", "r") }:same { nil, "/x: No such file" }
            end)

            it("reads a single byte", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, "r")
                expect(handle.read()):eq("a")
                handle.close()
            end)

            read_tests("r")
        end)

        describe("reading in binary mode", function()
            it("reads a single byte", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, "rb")
                expect(handle.read()):eq(97)
                handle.close()
            end)

            read_tests("rb")
        end)

        describe("opening in r+ mode", function()
            it("fails when reading non-files", function()
                expect { fs.open("x", "r+") }:same { nil, "/x: No such file" }
                expect { fs.open("", "r+") }:same { nil, "/: Not a file" }
            end)

            read_tests("r+")

            it("can read and write to a file", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, "r+")
                expect(handle.read(3)):eq("an ")

                handle.write("exciting file")
                expect(handle.seek("cur")):eq(16)

                handle.seek("set", 0)
                expect(handle.readAll()):eq("an exciting file")
            end)
        end)

        describe("writing", function()
            it("fails on directories", function()
                expect { fs.open("", "w") }:same { nil, "/: Cannot write to directory" }
            end)

            it("fails on read-only mounts", function()
                expect { fs.open("rom/x", "w") }:same { nil, "/rom/x: Access denied" }
            end)

            it("errors when closing twice", function()
                local handle = fs.open(test_file "out.txt", "w")
                handle.close()
                expect.error(handle.close):eq("attempt to use a closed file")
            end)

            it("fails gracefully when opening 'CON' on Windows", function()
                local ok, err = fs.open("test-files/con", "w")
                if ok then fs.delete("test-files/con") return end

                -- On my Windows/Java version the message appears to be "Incorrect function.". It may not be
                -- consistent though, and honestly doesn't matter too much.
                expect(err):str_match("^/test%-files/con: .*")
            end)

            it("writing numbers coerces them to a string", function()
                local handle = fs.open(test_file "out.txt", "w")
                handle.write(65)
                handle.close()

                local handle = fs.open(test_file "out.txt", "r")
                expect(handle.readAll()):eq("65")
                handle.close()
            end)

            it("can write lines", function()
                local handle = fs.open(test_file "out.txt", "w")
                handle.writeLine("First line!")
                handle.writeLine("Second line.")
                handle.close()

                local handle = fs.open(test_file "out.txt", "r")
                expect(handle.readLine()):eq("First line!")
                expect(handle.readLine()):eq("Second line.")
                expect(handle.readLine()):eq(nil)
                handle.close()
            end)
        end)

        describe("writing in binary mode", function()
            it("errors when closing twice", function()
                local handle = fs.open("test-files/out.txt", "wb")
                handle.close()
                expect.error(handle.close):eq("attempt to use a closed file")
            end)

            it("writing numbers treats them as bytes", function()
                local handle = fs.open(test_file "out.txt", "wb")
                handle.write(65)
                handle.close()

                local handle = fs.open(test_file "out.txt", "rb")
                expect(handle.readAll()):eq("A")
                handle.close()
            end)
        end)

        describe("opening in w+ mode", function()
            it("can write a file", function()
                local handle = fs.open(test_file "out.txt", "w+")
                handle.write("hello")
                handle.seek("set", 0)
                expect(handle.readAll()):eq("hello")

                handle.write(", world!")
                handle.seek("set", 0)
                handle.write("H")

                handle.seek("set", 0)
                expect(handle.readAll()):eq("Hello, world!")
            end)

            it("truncates an existing file", function()
                local file = create_test_file "an example file"

                local handle = fs.open(file, "w+")
                expect(handle.readAll()):eq("")
            end)
        end)

        describe("appending", function()
            it("fails on directories", function()
                expect { fs.open("", "a") }:same { nil, "/: Cannot write to directory" }
            end)

            it("fails on read-only mounts", function()
                expect { fs.open("rom/x", "a") }:same { nil, "/rom/x: Access denied" }
            end)
        end)
    end)

    describe("fs.makeDir", function()
        it("fails on files", function()
            expect.error(fs.makeDir, "startup.lua"):eq("/startup.lua: File exists")
        end)

        it("fails on read-only mounts", function()
            expect.error(fs.makeDir, "rom/x"):eq("/rom/x: Access denied")
        end)
    end)

    describe("fs.delete", function()
        it("fails on read-only mounts", function()
            expect.error(fs.delete, "rom/x"):eq("/rom/x: Access denied")
        end)
    end)

    describe("fs.copy", function()
        it("fails on read-only mounts", function()
            expect.error(fs.copy, "rom", "rom/startup"):eq("/rom/startup: Access denied")
        end)

        it("fails to copy a folder inside itself", function()
            fs.makeDir("some-folder")
            expect.error(fs.copy, "some-folder", "some-folder/x"):eq("/some-folder: Can't copy a directory inside itself")
            expect.error(fs.copy, "some-folder", "Some-Folder/x"):eq("/some-folder: Can't copy a directory inside itself")
        end)

        it("copies folders", function()
            fs.delete("some-folder")
            fs.delete("another-folder")

            fs.makeDir("some-folder")
            fs.copy("some-folder", "another-folder")
            expect(fs.isDir("another-folder")):eq(true)
        end)
    end)

    describe("fs.move", function()
        it("fails on read-only mounts", function()
            expect.error(fs.move, "rom", "rom/move"):eq("Access denied")
            expect.error(fs.move, "test-files", "rom/move"):eq("Access denied")
            expect.error(fs.move, "rom", "test-files"):eq("Access denied")
        end)

        it("fails if source does not exist", function()
            expect.error(fs.move, test_file "src", test_file "dest"):eq("No such file")
        end)

        it("fails if destination exists", function()
            fs.open(test_file "src", "w").close()
            fs.open(test_file "dest", "w").close()

            expect.error(fs.move, test_file "src", test_file "dest"):eq("File exists")
        end)

        it("fails to move a directory inside itself", function()
            fs.open(test_file "file", "w").close()
            expect.error(fs.move, test_root, test_file "child"):eq("Can't move a directory inside itself")
            expect.error(fs.move, "", "child"):eq("Can't move a directory inside itself")
        end)

        it("files can be renamed", function()
            fs.open(test_file "src", "w").close()
            fs.move(test_file "src",  test_file" dest")

            expect(fs.exists(test_file "src")):eq(false)
            expect(fs.exists(test_file "dest")):eq(true)
        end)

        it("directories can be renamed", function()
            fs.open(test_file "src/some/file", "w").close()
            fs.move(test_file "src",  test_file" dest")

            expect(fs.exists(test_file "src")):eq(false)
            expect(fs.exists(test_file "dest")):eq(true)
            expect(fs.exists(test_file "dest/some/file")):eq(true)
        end)

        it("creates directories before renaming", function()
            fs.open(test_file "src", "w").close()
            fs.move(test_file "src", test_file "dest/file")

            expect(fs.exists(test_file "src")):eq(false)
            expect(fs.exists(test_file "dest/file")):eq(true)
        end)
    end)

    describe("fs.getCapacity", function()
        it("returns nil on read-only mounts", function()
            expect(fs.getCapacity("rom")):eq(nil)
        end)

        it("returns the capacity on the root mount", function()
            expect(fs.getCapacity("")):eq(10000000)
        end)
    end)

    describe("fs.attributes", function()
        it("errors on non-existent files", function()
            expect.error(fs.attributes, "xuxu_nao_existe"):eq("/xuxu_nao_existe: No such file")
        end)

        it("returns information about read-only mounts", function()
            expect(fs.attributes("rom")):matches { isDir = true, size = 0, isReadOnly = true }
        end)

        it("returns information about files", function()
            local now = os.epoch("utc")

            local h = fs.open(test_file "basic-file", "w")
            h.write("A reasonably sized string")
            h.close()

            local attributes = fs.attributes(test_file "basic-file")
            expect(attributes):matches { isDir = false, size = 25, isReadOnly = false }

            if attributes.created - now >= 1000 then
                fail(("Expected created time (%d) to be within 1000ms of now (%d"):format(attributes.created, now))
            end

            if attributes.modified - now >= 1000 then
                fail(("Expected modified time (%d) to be within 1000ms of now (%d"):format(attributes.modified, now))
            end

            expect(attributes.modification):eq(attributes.modified)
        end)
    end)
end)
