-- SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- A minimal implementation of require.

This is intended for use with APIs, and other internal code which is not run in
the [`shell`] environment. This allows us to avoid some of the overhead of
loading the full [`cc.require`] module.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

@local

@tparam string name The module to require.
@return The required module.
]]

local loaded = {}
local env = setmetatable({}, { __index = _G })
local function require(name)
    local result = loaded[name]
    if result then return result end

    local path = "rom/modules/main/" .. name:gsub("%.", "/")
    if fs.exists(path .. ".lua") then
        result = assert(loadfile(path .. ".lua", nil, env))()
    else
        result = assert(loadfile(path .. "/init.lua", nil, env))()
    end
    loaded[name] = result
    return result
end
env.require = require
return require
