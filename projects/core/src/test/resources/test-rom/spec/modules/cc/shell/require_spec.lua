describe("cc.require", function()
    local r = require "cc.require"
    local function mk()
        local env = setmetatable({}, { __index = _ENV })
        env.require, env.package = r.make({}, "/test-files/modules")
        return env.require, env.package
    end

    local function setup(path, contents)
        fs.delete("/test-files/modules")
        io.open(path, "w"):write(contents):close()
    end

    describe("require", function()
        it("errors on recursive modules", function()
            local require, package = mk()
            package.preload.pkg = function() require "pkg" end
            expect.error(require, "pkg"):eq("loop or previous error loading module 'pkg'")
        end)

        it("supplies the current module name", function()
            local require, package = mk()
            package.preload.pkg = table.pack
            expect(require("pkg")):same { n = 1, "pkg" }
        end)

        it("returns true instead of nil", function()
            local require, package = mk()
            package.preload.pkg = function() return nil end
            expect(require("pkg")):eq(true)
        end)

        it("returns a constant value", function()
            local require, package = mk()
            package.preload.pkg = function() return {} end
            expect(require("pkg")):eq(require("pkg"))
        end)

        it("returns an error on not-found modules", function()
            local require, package = mk()
            package.path = "/?;/?.lua"
            expect.error(require, "pkg"):eq(
                "module 'pkg' not found:\n" ..
                "  no field package.preload['pkg']\n" ..
                "  no file '/pkg'\n" ..
                "  no file '/pkg.lua'")
        end)
    end)

    describe("the file loader", function()
        local function get(path)
            local require, package = mk()
            if path then package.path = path end
            return require
        end

        it("works on absolute paths", function()
            local require = get("/test-files/?.lua")
            setup("test-files/some_module.lua", "return 123")
            expect(require("some_module")):eq(123)
        end)

        it("works on relative paths", function()
            local require = get("?.lua")
            setup("test-files/modules/some_module.lua", "return 123")
            expect(require("some_module")):eq(123)
        end)

        it("fails on syntax errors", function()
            local require = get("?.lua")
            setup("test-files/modules/some_module.lua", "1")
            expect.error(require, "some_module"):str_match(
                "^module 'some_module' not found:\n" ..
                "  no field package.preload%['some_module'%]\n" ..
                "  [^:]*some_module.lua:1: unexpected symbol near '1'$"
            )
        end)
    end)
end)
