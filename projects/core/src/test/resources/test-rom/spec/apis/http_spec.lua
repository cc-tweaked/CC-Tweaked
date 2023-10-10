-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local timeout = require "test_helpers".timeout

describe("The http library", function()
    describe("http.checkURL", function()
        it("accepts well formed domains", function()
            expect({ http.checkURL("https://google.com") }):same({ true })
        end)

        it("rejects malformed URLs", function()
            expect({ http.checkURL("google.com") }):same({ false, "Must specify http or https" })
            expect({ http.checkURL("https:google.com") }):same({ false, "URL malformed" })
            expect({ http.checkURL("https:/google.com") }):same({ false, "URL malformed" })
            expect({ http.checkURL("wss://google.com") }):same({ false, "Invalid protocol 'wss'" })
        end)

        it("rejects local domains", function()
            -- Note, this is tested more thoroughly in AddressRuleTest. We've just got this here
            -- to ensure the general control flow works.
            expect({ http.checkURL("http://localhost") }):same({ false, "Domain not permitted" })
            expect({ http.checkURL("http://127.0.0.1") }):same({ false, "Domain not permitted" })
        end)
    end)

    describe("http.websocketAsync", function()
        it("queues an event for immediate failures", function()
            timeout(1, function()
                local url = "http://not.a.websocket"
                http.websocketAsync(url)
                local _, url2, message = os.pullEvent("websocket_failure")
                expect(url2):eq(url)
                expect(message):eq("Invalid scheme 'http'")
            end)
        end)
    end)

    describe("http.requestAsync", function()
        it("queues an event for immediate failures", function()
            timeout(1, function()
                local url = "ws://not.a.request"
                http.request(url)
                local _, url2, message = os.pullEvent("http_failure")
                expect(url2):eq(url)
                expect(message):eq("Invalid protocol 'ws'")
            end)
        end)
    end)
end)
