describe("The colors library", function()
    describe("colors.combine", function()
        it("validates arguments", function()
            expect.error(colors.combine, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(colors.combine, 1, 1, nil):eq("bad argument #3 (expected number, got nil)")
        end)

        it("combines colours", function()
            expect(colors.combine()):eq(0)
            expect(colors.combine(colors.red, colors.brown, colors.green)):eq(0x7000)
        end)
    end)

    describe("colors.subtract", function()
        it("validates arguments", function()
            expect.error(colors.subtract, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(colors.subtract, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(colors.subtract, 1, 1, nil):eq("bad argument #3 (expected number, got nil)")
        end)

        it("subtracts colours", function()
            expect(colors.subtract(0x7000, colors.green)):equals(0x5000)
            expect(colors.subtract(0x5000, colors.red)):equals(0x1000)
        end)
        it("does nothing when color is not present", function()
            expect(colors.subtract(0x1000, colors.red)):equals(0x1000)
        end)
        it("accepts multiple arguments", function()
            expect(colors.subtract(0x7000, colors.red, colors.green, colors.red)):equals(0x1000)
        end)
    end)

    describe("colors.test", function()
        it("validates arguments", function()
            expect.error(colors.test, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(colors.test, 1, nil):eq("bad argument #2 (expected number, got nil)")
        end)

        it("returns true when present", function()
            expect(colors.test(0x7000, colors.green)):equals(true)
        end)
        it("returns false when absent", function()
            expect(colors.test(0x5000, colors.green)):equals(false)
        end)
        it("allows multiple colors", function()
            expect(colors.test(0x7000, 0x5000)):equals(true)
        end)
    end)

    describe("colors.packRGB", function()
        it("validates arguments", function()
            expect.error(colors.packRGB, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(colors.packRGB, 1, nil):eq("bad argument #2 (expected number, got nil)")
            expect.error(colors.packRGB, 1, 1, nil):eq("bad argument #3 (expected number, got nil)")
        end)

        it("packs colours", function()
            expect(colors.packRGB(0.3, 0.5, 0.6)):equals(0x4c7f99)
        end)
    end)

    describe("colors.unpackRGB", function()
        it("validates arguments", function()
            expect.error(colors.unpackRGB, nil):eq("bad argument #1 (expected number, got nil)")
        end)

        it("unpacks colours", function()
            expect({ colors.unpackRGB(0x4c7f99) }):same { 0x4c / 0xFF, 0x7f / 0xFF, 0.6 }
        end)
    end)

    it("colors.rgb8", function()
        expect(colors.rgb8(0.3, 0.5, 0.6)):equals(0x4c7f99)
        expect({ colors.rgb8(0x4c7f99) }):same { 0x4c / 0xFF, 0x7f / 0xFF, 0.6 }
    end)

    describe("colors.toBlit", function()
        it("validates arguments", function()
            expect.error(colors.toBlit, nil):eq("bad argument #1 (expected number, got nil)")
        end)

        it("converts all colors", function()
            local colorsHex = {
                [colors.white] = "0",
                [colors.orange] = "1",
                [colors.magenta] = "2",
                [colors.lightBlue] = "3",
                [colors.yellow] = "4",
                [colors.lime] = "5",
                [colors.pink] = "6",
                [colors.gray] = "7",
                [colors.lightGray] = "8",
                [colors.cyan] = "9",
                [colors.purple] = "a",
                [colors.blue] = "b",
                [colors.brown] = "c",
                [colors.green] = "d",
                [colors.red] = "e",
                [colors.black] = "f",
            }

            for color, hex in pairs(colorsHex) do
                expect(colors.toBlit(color)):eq(hex)
            end
        end)

        it("floors colors", function()
            expect(colors.toBlit(16385)):eq("e")
        end)
    end)
end)
