local function it_side(func, ...)
    local arg = table.pack(...)
    it("requires a specific side", function()
        expect.error(func, 0):eq("bad argument #1 (string expected, got number)")
        expect.error(func, "blah", table.unpack(arg)):eq("bad argument #1 (unknown option blah)")

        func("top", table.unpack(arg))
        func("Top", table.unpack(arg))
        func("toP", table.unpack(arg))
    end)
end

describe("The redstone library", function()
    describe("redstone.setOutput", function()
        it_side(redstone.setOutput, false)

        it("sets the output strength correctly", function()
            redstone.setOutput("top", false)
            expect(redstone.getAnalogueOutput("top")):eq(0)

            redstone.setOutput("top", true)
            expect(redstone.getAnalogueOutput("top")):eq(15)
        end)
    end)

    describe("redstone.getOutput", function()
        it_side(redstone.getOutput)

        it("gets the output strength correctly", function()
            redstone.setAnalogueOutput("top", 0)
            expect(redstone.getOutput("top")):eq(false)

            redstone.setAnalogueOutput("top", 1)
            expect(redstone.getOutput("top")):eq(true)

            redstone.setAnalogueOutput("top", 15)
            expect(redstone.getOutput("top")):eq(true)
        end)
    end)

    describe("redstone.getInput", function()
        it_side(redstone.getInput)
    end)

    describe("redstone.setAnalogueOutput", function()
        it_side(redstone.setAnalogueOutput, 0)

        it("checks the strength parameter", function()
            expect.error(redstone.setAnalogueOutput, "top", true):eq("bad argument #2 (number expected, got boolean)")
            expect.error(redstone.setAnalogueOutput, "top", 0 / 0):eq("bad argument #2 (number expected, got nan)")
            expect.error(redstone.setAnalogueOutput, "top", math.huge):eq("bad argument #2 (number expected, got inf)")
            expect.error(redstone.setAnalogueOutput, "top", -1):eq("Expected number in range 0-15")
            expect.error(redstone.setAnalogueOutput, "top", 16):eq("Expected number in range 0-15")
        end)
    end)

    describe("redstone.getAnalogueOutput", function()
        it_side(redstone.getAnalogueOutput)
    end)

    describe("redstone.getAnalogueInput", function()
        it_side(redstone.getAnalogueInput)
    end)

    describe("redstone.setBundledOutput", function()
        it_side(redstone.setBundledOutput, 0)

        it("checks the mask parameter", function()
            expect.error(redstone.setBundledOutput, "top", true):eq("bad argument #2 (number expected, got boolean)")
            expect.error(redstone.setBundledOutput, "top", 0 / 0):eq("bad argument #2 (number expected, got nan)")
            expect.error(redstone.setBundledOutput, "top", math.huge):eq("bad argument #2 (number expected, got inf)")
        end)
    end)

    describe("redstone.getBundledOutput", function()
        it_side(redstone.getBundledOutput)
    end)

    describe("redstone.getBundledInput", function()
        it_side(redstone.getBundledInput)
    end)

    describe("redstone.testBundledInput", function()
        it_side(redstone.testBundledInput, 0)

        it("checks the mask parameter", function()
            expect.error(redstone.testBundledInput, "top", true):eq("bad argument #2 (number expected, got boolean)")
            expect.error(redstone.testBundledInput, "top", 0 / 0):eq("bad argument #2 (number expected, got nan)")
            expect.error(redstone.testBundledInput, "top", math.huge):eq("bad argument #2 (number expected, got inf)")
        end)
    end)
end)
