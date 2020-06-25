local helpers = require "test_helpers"

describe("cc.image.nft", function()
    local nft = require("cc.image.nft")

    describe("parse", function()
        it("validates arguments", function()
            nft.parse("")
            expect.error(nft.parse, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("parses an empty string", function()
            expect(nft.parse("")):same {}
        end)

        it("parses a string with no colours", function()
            expect(nft.parse("Hello")):same { { text = "Hello", foreground = "00000", background = "fffff" } }
        end)

        it("handles background and foreground colours", function()
            expect(nft.parse("\30a\31bHello"))
                :same { { text = "Hello", foreground = "bbbbb", background = "aaaaa" } }
        end)

        it("parses multi-line files", function()
            expect(nft.parse("Hello\nWorld")):same {
                { text = "Hello", foreground = "00000", background = "fffff" },
                { text = "World", foreground = "00000", background = "fffff" },
            }
        end)

        it("handles empty lines", function()
            expect(nft.parse("\n\n")):same {
                { text = "", foreground = "", background = "" },
                { text = "", foreground = "", background = "" },
            }
        end)
    end)

    describe("load", function()
        it("validates arguments", function()
            nft.load("")
            expect.error(nft.load, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("loads from a file", function()
            local image = fs.open("/test-files/example.nft", "w")
            image.write("\30aHello, world!")
            image.close()

            expect(nft.load("/test-files/example.nft")):same {
                { background = "aaaaaaaaaaaaa", foreground = "0000000000000", text = "Hello, world!" },
            }
        end)

        it("fails on missing files", function()
            expect({ nft.load("/test-files/not_a_file.nft") })
                :same { nil, "/test-files/not_a_file.nft: No such file" }
        end)
    end)

    describe("draw", function()
        it("validates arguments", function()
            expect.error(nft.draw, nil):eq("bad argument #1 (expected table, got nil)")
            expect.error(nft.draw, {}, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(nft.draw, {}, 1, nil):eq("bad argument #3 (expected number, got nil)")
            expect.error(nft.draw, {}, 1, 1, false):eq("bad argument #4 (expected table, got boolean)")
        end)

        it("draws an image", function()
            local win = helpers.with_window(7, 3, function()
                nft.draw({
                    { background = "aaaaa", foreground = "f000f", text = "Hello" },
                }, 2, 2)
            end)

            expect(win.getLine(1)):eq("       ")
            expect({ win.getLine(2) }):same { " Hello ", "0f000f0", "faaaaaf" }
            expect(win.getLine(3)):eq("       ")
        end)

        it("draws an image to a custom redirect", function()
            local win = window.create(term.current(), 1, 1, 5, 1, false)
            nft.draw({
                { background = "aaaaa", foreground = "f000f", text = "Hello" },
            }, 1, 1, win)

            expect({ win.getLine(1) }):same { "Hello", "f000f", "aaaaa" }
        end)
    end)
end)
