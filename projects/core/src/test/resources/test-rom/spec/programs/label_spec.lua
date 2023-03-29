-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local capture = require "test_helpers".capture_program

describe("The label program", function()
    it("displays its usage when given no arguments", function()
        expect(capture("label"))
            :matches { ok = true, output = "Usages:\nlabel get\nlabel get <drive>\nlabel set <text>\nlabel set <drive> <text>\nlabel clear\nlabel clear <drive>\n", error = "" }
    end)

    describe("displays the computer's label", function()
        it("when it is not labelled", function()
            stub(os, "getComputerLabel", function() return nil end)
            expect(capture("label get"))
                :matches { ok = true, output = "No Computer label\n", error = "" }
        end)

        it("when it is labelled", function()
            stub(os, "getComputerLabel", function() return "Test" end)
            expect(capture("label get"))
                :matches { ok = true, output = "Computer label is \"Test\"\n", error = "" }
        end)
    end)

    it("sets the computer's label", function()
        local setComputerLabel = stub(os, "setComputerLabel")
        capture("label set Test")
        expect(setComputerLabel):called_with("Test")
    end)

    it("clears the computer's label", function()
        local setComputerLabel = stub(os, "setComputerLabel")
        capture("label clear")
        expect(setComputerLabel):called_with(nil)
    end)
end)
