-- SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("cc.base64", function()
    local base64 = require "cc.base64"

    it("random strings roundtrip", function()
        for _ = 1, 1000 do
            local len = math.random(500)
            local str = ""
            for _ = 1, len do str = str .. string.char(math.random(0, 255)) end

            expect(base64.decode(base64.encode(str))):eq(str)
        end
    end)

    describe("encode", function()
        it("validates arguments", function()
            expect.error(base64.encode, 2):eq("bad argument #1 (string expected, got number)")
            expect.error(base64.encode, "", 2):eq("bad argument #2 (string expected, got number)")
            expect.error(base64.encode, "", ""):eq("alt_chars must be exactly two characters")
        end)

        it("encodes as expected", function()
            expect(base64.encode("")):eq("")
            expect(base64.encode("light w")):eq("bGlnaHQgdw==")
            expect(base64.encode("light wo")):eq("bGlnaHQgd28=")
            expect(base64.encode("light wor")):eq("bGlnaHQgd29y")
            expect(base64.encode("Many hands make light work.")):eq("TWFueSBoYW5kcyBtYWtlIGxpZ2h0IHdvcmsu")
        end)

        it("encodes using alternative alphabet", function()
            expect(base64.encode("Test: \255\230")):eq("VGVzdDog/+Y=")
            expect(base64.encode("Test: \255\230", "-_")):eq("VGVzdDog_-Y=")
        end)
    end)

    describe("decode", function()
        it("validates arguments", function()
            expect.error(base64.decode, 2):eq("bad argument #1 (string expected, got number)")
            expect.error(base64.decode, "", 2):eq("bad argument #2 (string expected, got number)")
            expect.error(base64.decode, "", ""):eq("alt_chars must be exactly two characters")
        end)

        it("decodes as expected", function()
            expect(base64.decode("")):eq("")
            expect(base64.decode("bGlnaHQgdw==")):eq("light w")
            expect(base64.decode("bGlnaHQgd28=")):eq("light wo")
            expect(base64.decode("bGlnaHQgd29y")):eq("light wor")
            expect(base64.decode("TWFueSBoYW5kcyBtYWtlIGxpZ2h0IHdvcmsu")):eq("Many hands make light work.")
        end)

        it("decodes using alternative alphabet", function()
            expect(base64.decode("VGVzdDog/+Y=")):eq("Test: \255\230")
            expect(base64.decode("VGVzdDog_-Y=", "-_")):eq("Test: \255\230")
        end)

        it("validates the input string", function()
            expect { base64.decode("VGVzdDog/+Y") }:same { nil, "input is not valid base64" }
            expect { base64.decode("VGVzdDog/+Y==") }:same { nil, "input is not valid base64" }
            expect { base64.decode("VGVzdDog/=Y=") }:same { nil, "input is not valid base64" }
            expect { base64.decode("VGVzdDog/===") }:same { nil, "input is not valid base64" }
        end)
    end)
end)
