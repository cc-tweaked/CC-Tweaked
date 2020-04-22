local capture = require "test_helpers".capture_program

describe("The pastebin program", function()
    local function setup_request()
        stub(_G, "http", {
            checkURL = function()
                return true
            end,
            get = function()
                return {
                    readAll = function()
                        return [[print("Hello", ...)]]
                    end,
                    close = function()
                    end,
                    getResponseHeaders = function()
                        local tHeader = {}
                        tHeader["Content-Type"] = "text/plain; charset=utf-8"
                        return tHeader
                    end,
                }
            end,
            post = function()
                return {
                    readAll = function()
                        return "https://pastebin.com/abcde"
                    end,
                    close = function()
                    end,
                }
            end,
        })
    end

    it("downloads one file", function()
        setup_request()
        capture(stub, "pastebin", "get", "abcde", "testdown")

        expect(fs.exists("/testdown")):eq(true)
    end)

    it("runs a program from the internet", function()
        setup_request()

        expect(capture(stub, "pastebin", "run", "abcde", "a", "b", "c"))
            :matches { ok = true, output = "Connecting to pastebin.com... Success.\nHello a b c\n", error = "" }
    end)

    it("upload a program to pastebin", function()
        setup_request()

        local file = fs.open("testup", "w")
        file.close()

        expect(capture(stub, "pastebin", "put", "testup"))
            :matches { ok = true, output = "Connecting to pastebin.com... Success.\nUploaded as https://pastebin.com/abcde\nRun \"pastebin get abcde\" to download anywhere\n", error = "" }
    end)

    it("upload a not existing program to pastebin", function()
        setup_request()

        expect(capture(stub, "pastebin", "put", "nothing"))
            :matches { ok = true, output = "No such file\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        setup_request()

        expect(capture(stub, "pastebin"))
            :matches { ok = true, output = "Usages:\npastebin put <filename>\npastebin get <code> <filename>\npastebin run <code> <arguments>\n", error = "" }
    end)

    it("can be completed", function()
        local complete = shell.getCompletionInfo()["rom/programs/http/pastebin.lua"].fnComplete
        expect(complete(shell, 1, "", {})):same { "put ", "get ", "run " }
    end)
end)
