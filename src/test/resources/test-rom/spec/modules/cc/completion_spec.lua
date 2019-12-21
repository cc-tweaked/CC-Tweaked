describe("cc.completion", function()
    local c = require("cc.completion")

    describe("choice", function()
        it("provides all choices", function()
            expect(c.choice("", { "some text", "some other", "other" }))
                :same { "some text", "some other", "other" }
        end)

        it("provides a filtered list of choices", function()
            expect(c.choice("som", { "some text", "some other", "other" }))
                :same { "e text", "e other" }

            expect(c.choice("none", { "some text", "some other", "other" }))
                :same { }
        end)

        it("adds text if needed", function()
            expect(c.choice("som", { "some text", "some other", "other" }, true))
                :same { "e text ", "e other " }
        end)
    end)

    describe("peripheral", function()
        it("provides a choice of peripherals", function()
            stub(peripheral, "getNames", function() return { "drive_0", "left" } end)

            expect(c.peripheral("dri")):same { "ve_0" }
            expect(c.peripheral("dri", true)):same { "ve_0 " }
        end)
    end)

    describe("side", function()
        it("provides a choice of sides", function()
            expect(c.side("le")):same { "ft" }
            expect(c.side("le", true)):same { "ft " }
        end)
    end)

    describe("setting", function()
        it("provides a choice of setting names", function()
            stub(settings, "getNames", function() return { "shell.allow_startup", "list.show_hidden" } end)

            expect(c.setting("li")):same { "st.show_hidden" }
            expect(c.setting("li", true)):same { "st.show_hidden " }
        end)
    end)

    describe("command", function()
        it("provides a choice of command names", function()
            stub(_G, "commands", { list = function() return { "list", "say" } end })

            expect(c.command("li")):same { "st" }
            expect(c.command("li", true)):same { "st " }
        end)
    end)
end)
