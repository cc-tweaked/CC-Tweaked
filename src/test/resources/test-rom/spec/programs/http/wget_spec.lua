local capture = require "test_helpers".capture_program

describe("The wget program", function()
    local default_contents = [[print("Hello", ...)]]
    local function setup_request(contents)
        stub(_G, "http", {
            checkURL = function()
                return true
            end,
            get = function()
                return {
                    readAll = function()
                        return contents
                    end,
                    close = function()
                    end,
                }
            end,
        })
    end

    it("downloads one file", function()
        fs.delete("/example.com")
        setup_request(default_contents)

        capture(stub, "wget", "https://example.com")

        expect(fs.exists("/example.com")):eq(true)
    end)

    it("downloads one file with given filename", function()
        fs.delete("/test-files/download")
        setup_request(default_contents)

        capture(stub, "wget", "https://example.com /test-files/download")

        expect(fs.exists("/test-files/download")):eq(true)
    end)

    it("downloads empty files", function()
        fs.delete("/test-files/download")
        setup_request(nil)

        capture(stub, "wget", "https://example.com", "/test-files/download")

        expect(fs.exists("/test-files/download")):eq(true)
        expect(fs.getSize("/test-files/download")):eq(0)
    end)

    it("cannot save to rom", function()
        setup_request(default_contents)

        expect(capture(stub, "wget", "https://example.com", "/rom/a-file.txt")):matches {
            ok = true,
            output = "Connecting to https://example.com... Success.\n",
            error = "Cannot save file: /rom/a-file.txt: Access denied\n",
        }
    end)

    it("runs a program from the internet", function()
        setup_request(default_contents)

        expect(capture(stub, "wget", "run", "http://test.com", "a", "b", "c"))
            :matches { ok = true, output = "Connecting to http://test.com... Success.\nHello a b c\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        setup_request(default_contents)

        expect(capture(stub, "wget"))
            :matches { ok = true, output = "Usage:\nwget <url> [filename]\nwget run <url>\n", error = "" }
    end)

    it("can be completed", function()
        local complete = shell.getCompletionInfo()["rom/programs/http/wget.lua"].fnComplete
        expect(complete(shell, 1, "", {})):same { "run " }
    end)
end)
