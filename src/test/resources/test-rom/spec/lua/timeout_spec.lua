describe("The VM terminates long running code :slow", function()
    it("in loops", function()
        expect.error(function() while true do end end)
            :str_match("^.+:%d+: Too long without yielding$")
    end)

    describe("in string pattern matching", function()
        local str, pat = ("a"):rep(1e4), ".-.-.-.-b$"

        it("string.find", function()
            expect.error(string.find, str, pat):eq("Too long without yielding")
        end)
        it("string.match", function()
            expect.error(string.match, str, pat):eq("Too long without yielding")
        end)
    end)
end)
