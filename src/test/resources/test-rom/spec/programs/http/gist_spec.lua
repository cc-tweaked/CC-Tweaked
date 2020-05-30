local capture = require "test_helpers".capture_program

describe("The gist program", function()
    local function setup_request()
        stub(_G, "http", {
            checkURL = function()
                return true
            end,
            get = function()
                local file = fs.open("test-rom/data/gist-data.json", "r")
                file.getResponseHeaders = function()
                    local tHeader = {}
                    tHeader["Content-Type"] = "application/json"
                    return tHeader
                end
                file.getResponseCode = function()
                    return 200
                end
                return file
            end,
            post = function(tab)
                local file = fs.open("test-rom/data/gist-data.json", "r")
                if type(tab) == "table" and tab.method == "PATCH" then
                    local oldReadAll = file.readAll
                    file.readAll = function()
                        return oldReadAll()
                            :gsub('"hello_world.rb": %b{},\n', "")
                            :gsub('"Hello World Examples"', '"Hello World Examples (Updated)"'), nil
                    end
                end
                file.getResponseCode = function()
                    if type(tab) ~= "table" then return 201
                    elseif tab.method == "PATCH" then return 200
                    else return 204 end
                end
                return file
            end,
        })
        stub(_G, "settings", {
            get = function()
                return "0123456789abcdef"
            end,
        })
    end

    it("downloads one file", function()
        setup_request()
        capture(stub, "gist", "get", "aa5a315d61ae9438b18d", "testdown")

        expect(fs.exists("/testdown")):eq(true)
    end)

    it("runs a program from the internet", function()
        setup_request()

        expect(capture(stub, "gist", "run", "aa5a315d61ae9438b18d", "a", "b", "c"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nHello a b c\n", error = "" }
    end)

    it("gets info about a file", function()
        setup_request()
        expect(capture(stub, "gist", "info", "aa5a315d61ae9438b18d"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nDescription: Hello World Examples\nAuthor: octocat\nRevisions: 1\nFiles in this Gist:\n\n\n", error = "" } -- No file info because it uses positioning functions
    end)

    it("upload a program to Gist", function()
        setup_request()

        local file = fs.open("testup", "w")
        file.close()

        expect(capture(stub, "gist", "put", "testup"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nUploaded as https://gist.github.com/aa5a315d61ae9438b18d\nRun 'gist get aa5a315d61ae9438b18d' to download anywhere\n", error = "" }
    end)

    it("upload a not existing program to Gist", function()
        setup_request()

        expect(capture(stub, "gist", "put", "nothing"))
            :matches { ok = true, output = "Could not read nothing.\n", error = "" }
    end)

    it("edit a Gist", function()
        setup_request()

        expect(capture(stub, "gist", "edit", "aa5a315d61ae9438b18d", "hello_world.rb", "--", "Hello", "World", "Examples", "(Updated)"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nUploaded as https://gist.github.com/aa5a315d61ae9438b18d\nRun 'gist get aa5a315d61ae9438b18d' to download anywhere\n", error = "" }
    end)

    it("delete a Gist", function()
        setup_request()

        expect(capture(stub, "gist", "delete", "aa5a315d61ae9438b18d"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nThe requested Gist has been deleted.\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        setup_request()

        expect(capture(stub, "gist"))
            :matches { ok = true, output = "Usages:\ngist put <files...> [-- description...]\ngist edit <id> <files...> [-- description]\ngist delete <id>\ngist get <id> <filename>\ngist run <id> [arguments...]\ngist info <id>\n", error = "" }
    end)

    it("can be completed", function()
        local complete = shell.getCompletionInfo()["rom/programs/http/gist.lua"].fnComplete
        expect(complete(shell, 1, "", {})):same { "put ", "edit ", "delete ", "get ", "run ", "info " }
    end)
end)
