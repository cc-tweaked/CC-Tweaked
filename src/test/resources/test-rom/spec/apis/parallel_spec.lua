local function mock() end
local function longMock() os.pullEvent() end

describe("The parallel library", function()
    describe("parallel.waitForAny", function()
        it("validates arguments", function()
            parallel.waitForAny(mock, mock)
            parallel.waitForAny({mock, mock})

            expect.error(parallel.waitForAny, nil):eq("bad argument #1 (expected function, got nil)")
            expect.error(parallel.waitForAny, mock, nil):eq("bad argument #2 (expected function, got nil)")
            expect.error(parallel.waitForAny, {nil}):eq("bad argument #1 (expected function, got nil)")
            expect.error(parallel.waitForAny, {mock, nil}):eq("bad argument #2 (expected function, got nil)")
        end)
        it("returns the arg number of the function which finished first", function()
            expect(parallel.waitForAny, mock, mock):eq(1)
            expect(parallel.waitForAny, {mock, mock}):eq(1)
            
            expect(parallel.waitForAny, longMock, mock):eq(2)
            expect(parallel.waitForAny, {longMock, mock}):eq(2)
            
            expect(parallel.waitForAny, mock, longMock):eq(1)
            expect(parallel.waitForAny, {mock, longMock}):eq(1)
        end)
    end)
    
    describe("parallel.waitForAll", function()
        it("validates arguments", function()
            parallel.waitForAll(mock, mock)
            parallel.waitForAll({mock, mock})

            expect.error(parallel.waitForAll, nil):eq("bad argument #1 (expected function, got nil)")
            expect.error(parallel.waitForAll, mock, nil):eq("bad argument #2 (expected function, got nil)")
            expect.error(parallel.waitForAll, {nil}):eq("bad argument #1 (expected function, got nil)")
            expect.error(parallel.waitForAll, {mock, nil}):eq("bad argument #2 (expected function, got nil)")
        end)
        it("returns nil", function()
            expect(parallel.waitForAll, mock, mock):eq(nil)
            expect(parallel.waitForAll, {mock, mock}):eq(nil)
            
            expect(parallel.waitForAll, longMock, mock):eq(nil)
            expect(parallel.waitForAll, {longMock, mock}):eq(nil)
            
            expect(parallel.waitForAll, mock, longMock):eq(nil)
            expect(parallel.waitForAll, {mock, longMock}):eq(nil)
        end)
    end)
end)
