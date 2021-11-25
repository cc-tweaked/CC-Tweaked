describe("The fs library", function()
    describe("fs.complete", function()
        it("validates arguments", function()
            fs.complete("", "")
            fs.complete("", "", true)
            fs.complete("", "", nil, true)

            expect.error(fs.complete, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(fs.complete, "", nil):eq("bad argument #2 (expected string, got nil)")
            expect.error(fs.complete, "", "", 1):eq("bad argument #3 (expected boolean, got number)")
            expect.error(fs.complete, "", "", true, 1):eq("bad argument #4 (expected boolean, got number)")
        end)
    end)

    describe("fs.isDriveRoot", function()
        it("validates arguments", function()
            fs.isDriveRoot("")

            expect.error(fs.isDriveRoot, nil):eq("bad argument #1 (expected string, got nil)")
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
            expect.error(fs.list, "rom/x"):eq("/rom/x: Not a directory")
            expect.error(fs.list, "x"):eq("/x: Not a directory")
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
        describe("reading", function()
            it("fails on directories", function()
                expect { fs.open("rom", "r") }:same { nil, "/rom: No such file" }
                expect { fs.open("", "r") }:same { nil, "/: No such file" }
            end)

            it("fails on non-existent nodes", function()
                expect { fs.open("rom/x", "r") }:same { nil, "/rom/x: No such file" }
                expect { fs.open("x", "r") }:same { nil, "/x: No such file" }
            end)

            it("errors when closing twice", function()
                local handle = fs.open("rom/startup.lua", "r")
                handle.close()
                expect.error(handle.close):eq("attempt to use a closed file")
            end)
        end)

        describe("reading in binary mode", function()
            it("errors when closing twice", function()
                local handle = fs.open("rom/startup.lua", "rb")
                handle.close()
                expect.error(handle.close):eq("attempt to use a closed file")
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
                local handle = fs.open("test-files/out.txt", "w")
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
        end)

        describe("writing in binary mode", function()
            it("errors when closing twice", function()
                local handle = fs.open("test-files/out.txt", "wb")
                handle.close()
                expect.error(handle.close):eq("attempt to use a closed file")
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

            fs.delete("/tmp/basic-file")
            local h = fs.open("/tmp/basic-file", "w")
            h.write("A reasonably sized string")
            h.close()

            local attributes = fs.attributes("tmp/basic-file")
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
