describe("The VM", function()
    it("allows unpacking a large number of values", function()
        -- We don't allow arbitrarily many values, half a meg is probably fine.
        -- I don't actually have any numbers on this - maybe we do need more?
        local len = 2 ^ 19
        local tbl = { (" "):rep(len):byte(1, -1) }
        expect(#tbl):eq(len)
    end)
end)
