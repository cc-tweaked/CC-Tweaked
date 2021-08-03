describe("The os library", function()
    describe("os.date and os.time", function()
        it("round trips correctly", function()
            local t = math.floor(os.epoch("local") / 1000)
            local T = os.date("*t", t)

            expect(os.time(T)):eq(t)
        end)

        it("dst field is guessed", function()
            local T = os.date("*t")
            local t = os.time(T)
            expect(T.isdst):type("boolean")
            T.isdst = nil
            expect(os.time(T)):eq(t) -- if isdst is absent uses correct default
        end)

        it("has 365 days in a year", function()
            local T = os.date("*t")
            local t = os.time(T)
            T.year = T.year - 1
            local t1 = os.time(T)
            local delta = (t - t1) / (24 * 3600) - 365
            -- allow for leap years
            assert(math.abs(delta) < 2, ("expected abs(%d )< 2"):format(delta))
        end)

        it("os.date uses local timezone", function()
            local epoch = os.epoch("local") / 1000
            local date = os.time(os.date("*t"))
            assert(date - epoch <= 2, ("expected %d - %d <= 2, but not the case"):format(date, epoch))
        end)
    end)

    describe("os.date", function()
        it("formats as expected", function()
            -- From the PUC Lua tests, hence the weird style
            local t = os.epoch("local")
            local T = os.date("*t", t)

            _G.T = T
            loadstring(os.date([[assert(T.year==%Y and T.month==%m and T.day==%d and
              T.hour==%H and T.min==%M and T.sec==%S and
              T.wday==%w+1 and T.yday==%j and type(T.isdst) == 'boolean')]], t))()

            T = os.date("!*t", t)
            _G.T = T
            loadstring(os.date([[!assert(T.year==%Y and T.month==%m and T.day==%d and
              T.hour==%H and T.min==%M and T.sec==%S and
              T.wday==%w+1 and T.yday==%j and type(T.isdst) == 'boolean')]], t))()
        end)

        describe("produces output consistent with PUC Lua", function()
            -- Create a separate test for each code, just so it's easier to see what's broken
            local t1 = os.time { year = 2000, month = 10, day = 1, hour = 23, min = 12, sec = 17 }
            local function exp_code(code, value)
                it(("for code '%s'"):format(code), function()
                    expect(os.date(code, t1)):eq(value)
                end)
            end

            -- TODO: Java 16 apparently no longer treats TextStyle.FULL as full and will render Sun instead of Sunday.
            exp_code("%a", "Sun")
            -- exp_code("%A", "Sunday")
            exp_code("%b", "Oct")
            -- exp_code("%B", "October")
            exp_code("%c", "Sun Oct  1 23:12:17 2000")
            exp_code("%C", "20")
            exp_code("%d", "01")
            exp_code("%D", "10/01/00")
            exp_code("%e", " 1")
            exp_code("%F", "2000-10-01")
            exp_code("%g", "00")
            exp_code("%G", "2000")
            exp_code("%h", "Oct")
            exp_code("%H", "23")
            exp_code("%I", "11")
            exp_code("%j", "275")
            exp_code("%m", "10")
            exp_code("%M", "12")
            exp_code("%n", "\n")
            exp_code("%p", "PM")
            exp_code("%r", "11:12:17 PM")
            exp_code("%R", "23:12")
            exp_code("%S", "17")
            exp_code("%t", "\t")
            exp_code("%T", "23:12:17")
            exp_code("%u", "7")
            exp_code("%U", "40")
            exp_code("%V", "39")
            exp_code("%w", "0")
            exp_code("%W", "39")
            exp_code("%x", "10/01/00")
            exp_code("%X", "23:12:17")
            exp_code("%y", "00")
            exp_code("%Y", "2000")
            exp_code("%%", "%")

            it("zones are numbers", function()
                local zone = os.date("%z", t1)
                if not zone:match("^[+-]%d%d%d%d$") then
                    error("Invalid zone: " .. zone)
                end
            end)

            it("zones id is made of letters", function()
                local zone = os.date("%Z", t1)
                if not zone:match("^%a%a+$") then
                    error("Non letter character in zone: " .. zone)
                end
            end)

            local t2 = os.time { year = 2000, month = 10, day = 1, hour = 3, min = 12, sec = 17 }
            it("for code '%I' #2", function()
                expect(os.date("%I", t2)):eq("03")
            end)
        end)
    end)

    describe("os.time", function()
        it("maps directly to seconds", function()
            local t1 = os.time { year = 2000, month = 10, day = 1, hour = 23, min = 12, sec = 17 }
            local t2 = os.time { year = 2000, month = 10, day = 1, hour = 23, min = 10, sec = 19 }
            expect(t1 - t2):eq(60 * 2 - 2)
        end)
    end)

    describe("os.loadAPI", function()
        it("validates arguments", function()
            expect.error(os.loadAPI, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("os.unloadAPI", function()
        it("validates arguments", function()
            expect.error(os.loadAPI, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)
end)
