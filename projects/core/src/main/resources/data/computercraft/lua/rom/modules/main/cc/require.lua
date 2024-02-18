-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- A pure Lua implementation of the builtin [`require`] function and
[`package`] library.

Generally you do not need to use this module - it is injected into the every
program's environment. However, it may be useful when building a custom shell or
when running programs yourself.

@module cc.require
@since 1.88.0
@see using_require For an introduction on how to use [`require`].
@usage Construct the package and require function, and insert them into a
custom environment.

    local r = require "cc.require"
    local env = setmetatable({}, { __index = _ENV })
    env.require, env.package = r.make(env, "/")

    -- Now we have our own require function, separate to the original.
    local r2 = env.require "cc.require"
    print(r, r2)
]]

local expect = require and require("cc.expect") or dofile("rom/modules/main/cc/expect.lua")
local expect = expect.expect

local function preload(package)
    return function(name)
        if package.preload[name] then
            return package.preload[name]
        else
            return nil, "no field package.preload['" .. name .. "']"
        end
    end
end

local function from_file(package, env)
    return function(name)
        local sPath, sError = package.searchpath(name, package.path)
        if not sPath then
            return nil, sError
        end
        local fnFile, sError = loadfile(sPath, nil, env)
        if fnFile then
            return fnFile, sPath
        else
            return nil, sError
        end
    end
end

local function make_searchpath(dir)
    return function(name, path, sep, rep)
        expect(1, name, "string")
        expect(2, path, "string")
        sep = expect(3, sep, "string", "nil") or "."
        rep = expect(4, rep, "string", "nil") or "/"

        local fname = string.gsub(name, sep:gsub("%.", "%%%."), rep)
        local sError = ""
        for pattern in string.gmatch(path, "[^;]+") do
            local sPath = string.gsub(pattern, "%?", fname)
            if sPath:sub(1, 1) ~= "/" then
                sPath = fs.combine(dir, sPath)
            end
            if fs.exists(sPath) and not fs.isDir(sPath) then
                return sPath
            else
                if #sError > 0 then
                    sError = sError .. "\n  "
                end
                sError = sError .. "no file '" .. sPath .. "'"
            end
        end
        return nil, sError
    end
end

local function make_require(package)
    local sentinel = {}
    return function(name)
        expect(1, name, "string")

        if package.loaded[name] == sentinel then
            error("loop or previous error loading module '" .. name .. "'", 0)
        end

        if package.loaded[name] then
            return package.loaded[name]
        end

        local sError = "module '" .. name .. "' not found:"
        for _, searcher in ipairs(package.loaders) do
            local loader = table.pack(searcher(name))
            if loader[1] then
                package.loaded[name] = sentinel
                local result = loader[1](name, table.unpack(loader, 2, loader.n))
                if result == nil then result = true end

                package.loaded[name] = result
                return result
            else
                sError = sError .. "\n  " .. loader[2]
            end
        end
        error(sError, 2)
    end
end

--- Build an implementation of Lua's [`package`] library, and a [`require`]
-- function to load modules within it.
--
-- @tparam table env The environment to load packages into.
-- @tparam string dir The directory that relative packages are loaded from.
-- @treturn function The new [`require`] function.
-- @treturn table The new [`package`] library.
local function make_package(env, dir)
    expect(1, env, "table")
    expect(2, dir, "string")

    local package = {}
    package.loaded = {
        _G = _G,
        package = package,
    }

    -- Copy everything from the global package table to this instance.
    --
    -- This table is an internal implementation detail - it is NOT intended to
    -- be extended by user code.
    local registry = debug.getregistry()
    if registry and type(registry._LOADED) == "table" then
        for k, v in next, registry._LOADED do
            if type(k) == "string" then
                package.loaded[k] = v
            end
        end
    end

    package.path = "?;?.lua;?/init.lua;/rom/modules/main/?;/rom/modules/main/?.lua;/rom/modules/main/?/init.lua"
    if turtle then
        package.path = package.path .. ";/rom/modules/turtle/?;/rom/modules/turtle/?.lua;/rom/modules/turtle/?/init.lua"
    elseif commands then
        package.path = package.path .. ";/rom/modules/command/?;/rom/modules/command/?.lua;/rom/modules/command/?/init.lua"
    end
    package.config = "/\n;\n?\n!\n-"
    package.preload = {}
    package.loaders = { preload(package), from_file(package, env) }
    package.searchpath = make_searchpath(dir)

    return make_require(package), package
end

return { make = make_package }
