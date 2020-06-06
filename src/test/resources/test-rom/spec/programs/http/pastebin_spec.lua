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
    local function fake_http_post(fake_return)
        stub(_G, "http", {
            post = function()
                return {
                    readAll = function()
                        return fake_return
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

    it("pastebin list, pastebin infos and pastebin delete when disconnected", function()
        setup_request()

        expect(capture(stub, "pastebin", "list"))
            :matches { ok = true, output = "You aren't logged\nfirst use: pastebin connect\n", error = "" }
        expect(capture(stub, "pastebin", "infos"))
            :matches { ok = true, output = "You aren't logged\nfirst use: pastebin connect\n", error = "" }
        expect(capture(stub, "pastebin", "delete", "abcde"))
            :matches { ok = true, output = "You aren't logged\nfirst use: pastebin connect\n", error = "" }
    end)

    it("connect to pastebin", function()
        stub(_G, "read", function() return "read" end)
        fake_http_post("FakeL0ngUserKey")

        expect(capture(stub, "pastebin", "connect"))
            :matches { ok = true, output = "Username: Password: Connecting to pastebin.com... Success.\nFakeL0ngUserKey\n", error = "" }
    end)

    it("pastebin list when connected", function()
        fake_http_post("<paste>\n <paste_key>0b42rwhf</paste_key>\n <paste_date>1297953260</paste_date>\n <paste_title>javascript test</paste_title>\n <paste_size>15</paste_size>\n <paste_expire_date>1297956860</paste_expire_date>\n <paste_private>0</paste_private>\n <paste_format_long>JavaScript</paste_format_long>\n <paste_format_short>javascript</paste_format_short>\n <paste_url>https://pastebin.com/0b42rwhf</paste_url>\n <paste_hits>15</paste_hits>\n</paste>\n<paste>\n <paste_key>0C343n0d</paste_key>\n <paste_date>1297694343</paste_date>\n <paste_title>Welcome To Pastebin V3</paste_title>\n <paste_size>490</paste_size>\n <paste_expire_date>0</paste_expire_date>\n <paste_private>0</paste_private>\n <paste_format_long>None</paste_format_long>\n <paste_format_short>text</paste_format_short>\n <paste_url>https://pastebin.com/0C343n0d</paste_url>\n <paste_hits>65</paste_hits>\n</paste>\n")
        settings.set("pastebin.key", "FakeL0ngUserKey")

        expect(capture(stub, "pastebin", "list"))
            :matches { ok = true, output = "Connecting to pastebin.com... Success.\ncode     | name\n0b42rwhf | javascript test\n0C343n0d | Welcome To Pastebin V3\n", error = "" }

        settings.unset("pastebin.key")
    end)

    it("pastebin infos when connected", function()
        setup_request()
        fake_http_post("<user>\n <user_name>wiz_kitty</user_name>\n <user_format_short>text</user_format_short>\n <user_expiration>N</user_expiration>\n <user_avatar_url>https://pastebin.com/cache/a/1.jpg</user_avatar_url>\n <user_private>1</user_private> (0 Public, 1 Unlisted, 2 Private)\n <user_website>https://myawesomesite.com</user_website>\n <user_email>oh@dear.com</user_email>\n <user_location>New York</user_location>\n <user_account_type>1</user_account_type> (0 normal, 1 PRO)\n</user>\n")
        settings.set("pastebin.key", "FakeL0ngUserKey")

        expect(capture(stub, "pastebin", "infos"))
            :matches { ok = true, output = "Connecting to pastebin.com... Success.\nname: wiz_kitty\nformat_short: text\nexpiration: N\navatar_url: https://pastebin.com/cache/a/1.jpg\nprivate: 1\nwebsite: https://myawesomesite.com\nemail: oh@dear.com\nlocation: New York\naccount_type: 1\n", error = "" }

        settings.unset("pastebin.key")
   end)

    it("pastebin delete when connected", function()
        setup_request()
        fake_http_post("Paste Removed")
        settings.set("pastebin.key", "FakeL0ngUserKey")

        expect(capture(stub, "pastebin", "delete", "abcde"))
            :matches { ok = true, output = "Connecting to pastebin.com... Success.\nPaste Removed\n", error = "" }

        settings.unset("pastebin.key")
    end)

    it("disconnect to pastebin", function()
        setup_request()
        settings.set("pastebin.key", "FakeL0ngUserKey")

        expect(capture(stub, "pastebin", "disconnect"))
            :matches { ok = true, output = "Disconnected\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        setup_request()

        expect(capture(stub, "pastebin"))
            :matches { ok = true, output = "Usages:\npastebin put <filename>\npastebin get <code> <filename>\npastebin run <code> <arguments>\npastebin connect\npastebin disconnect\npastebin list\npastebin infos\npastebin delete <code>\n", error = "" }

    end)

    it("can be completed", function()
        local complete = shell.getCompletionInfo()["rom/programs/http/pastebin.lua"].fnComplete
        expect(complete(shell, 1, "", {})):same { "put ", "get ", "run ", "connect", "disconnect", "list", "infos", "delete " }
    end)
end)
