local capture = require "test_helpers".capture_program

describe("The wget program", function()
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
                }
            end,
        })
    end

    it("downloads one file", function()
        setup_request()
        capture(stub, "wget", "https://example.com")

        expect(fs.exists("/example.com")):eq(true)
    end)

    it("downloads one file with given filename", function()
        setup_request()
        capture(stub, "wget", "https://example.com /test-files/download")

        expect(fs.exists("/test-files/download")):eq(true)
    end)

    it("runs a program from the internet", function()
        setup_request()

        expect(capture(stub, "wget", "run", "http://test.com", "a", "b", "c"))
            :matches { ok = true, output = "Connecting to http://test.com... Success.\nHello a b c\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        setup_request()

        expect(capture(stub, "wget"))
            :matches { ok = true, output = "Usage:\nwget <url> [filename]\nwget run <url>\n", error = "" }
    end)

    it("can be completed", function()
        local complete = shell.getCompletionInfo()["rom/programs/http/wget.lua"].fnComplete
        expect(complete(shell, 1, "", {})):same { "run " }
    end)
end)
