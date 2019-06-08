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
    end)

    describe("fs.move", function()
        it("fails on read-only mounts", function()
            expect.error(fs.move, "rom", "rom/move"):eq("Access denied")
            expect.error(fs.move, "test-files", "rom/move"):eq("Access denied")
            expect.error(fs.move, "rom", "test-files"):eq("Access denied")
        end)
    end)
end)
