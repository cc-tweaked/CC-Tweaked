-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Read and write configuration options for CraftOS and your programs.

When a computer starts, it reads the current value of settings from the
`/.settings` file. These values then may be [read][`settings.get`] or
[modified][`settings.set`].

> [!WARNING]
> Calling [`settings.set`] does _not_ update the settings file by default. You
> _must_ call [`settings.save`] to persist values.

@module settings
@since 1.78
@usage Define an basic setting `123` and read its value.

    settings.define("my.setting", {
        description = "An example setting",
        default = 123,
        type = "number",
    })
    print("my.setting = " .. settings.get("my.setting")) -- 123

You can then use the `set` program to change its value (e.g. `set my.setting 456`),
and then re-run the `example` program to check it has changed.

]]

local expect = dofile("rom/modules/main/cc/expect.lua")
local type, expect, field = type, expect.expect, expect.field

local details, values = {}, {}

local function reserialize(value)
    if type(value) ~= "table" then return value end
    return textutils.unserialize(textutils.serialize(value))
end

local function copy(value)
    if type(value) ~= "table" then return value end
    local result = {}
    for k, v in pairs(value) do result[k] = copy(v) end
    return result
end

local valid_types = { "number", "string", "boolean", "table" }
for _, v in ipairs(valid_types) do valid_types[v] = true end

--- Define a new setting, optional specifying various properties about it.
--
-- While settings do not have to be added before being used, doing so allows
-- you to provide defaults and additional metadata.
--
-- @tparam string name The name of this option
-- @tparam[opt] { description? = string, default? = any, type? = string } options
-- Options for this setting. This table accepts the following fields:
--
--  - `description`: A description which may be printed when running the `set` program.
--  - `default`: A default value, which is returned by [`settings.get`] if the
--    setting has not been changed.
--  - `type`: Require values to be of this type. [Setting][`set`] the value to another type
--    will error.
-- @since 1.87.0
function define(name, options)
    expect(1, name, "string")
    expect(2, options, "table", "nil")

    if options then
        options = {
            description = field(options, "description", "string", "nil"),
            default = reserialize(field(options, "default", "number", "string", "boolean", "table", "nil")),
            type = field(options, "type", "string", "nil"),
        }

        if options.type and not valid_types[options.type] then
            error(("Unknown type %q. Expected one of %s."):format(options.type, table.concat(valid_types, ", ")), 2)
        end
    else
        options = {}
    end

    details[name] = options
end

--- Remove a [definition][`define`] of a setting.
--
-- If a setting has been changed, this does not remove its value. Use [`settings.unset`]
-- for that.
--
-- @tparam string name The name of this option
-- @since 1.87.0
function undefine(name)
    expect(1, name, "string")
    details[name] = nil
end

local function set_value(name, new)
    local old = values[name]
    if old == nil then
        local opt = details[name]
        old = opt and opt.default
    end

    values[name] = new
    if old ~= new then
        -- This should be safe, as os.queueEvent copies values anyway.
        os.queueEvent("setting_changed", name, new, old)
    end
end

--[[- Set the value of a setting.

> [!WARNING]
> Calling [`settings.set`] does _not_ update the settings file by default. You
> _must_ call [`settings.save`] to persist values.

@tparam string name The name of the setting to set
@param value The setting's value. This cannot be `nil`, and must be
serialisable by [`textutils.serialize`].
@throws If this value cannot be serialised
@see settings.unset
]]
function set(name, value)
    expect(1, name, "string")
    expect(2, value, "number", "string", "boolean", "table")

    local opt = details[name]
    if opt and opt.type then expect(2, value, opt.type) end

    set_value(name, reserialize(value))
end

--- Get the value of a setting.
--
-- @tparam string name The name of the setting to get.
-- @param[opt] default The value to use should there be pre-existing value for
-- this setting. If not given, it will use the setting's default value if given,
-- or `nil` otherwise.
-- @return The setting's, or the default if the setting has not been changed.
-- @changed 1.87.0 Now respects default value if pre-defined and `default` is unset.
function get(name, default)
    expect(1, name, "string")
    local result = values[name]
    if result ~= nil then
        return copy(result)
    elseif default ~= nil then
        return default
    else
        local opt = details[name]
        return opt and copy(opt.default)
    end
end

--- Get details about a specific setting.
--
-- @tparam string name The name of the setting to get.
-- @treturn { description? = string, default? = any, type? = string, value? = any }
-- Information about this setting. This includes all information from [`settings.define`],
-- as well as this setting's value.
-- @since 1.87.0
function getDetails(name)
    expect(1, name, "string")
    local deets = copy(details[name]) or {}
    deets.value = values[name]
    deets.changed = deets.value ~= nil
    if deets.value == nil then deets.value = deets.default end
    return deets
end

--- Remove the value of a setting, setting it to the default.
--
-- [`settings.get`] will return the default value until the setting's value is
-- [set][`settings.set`], or the computer is rebooted.
--
-- @tparam string name The name of the setting to unset.
-- @see settings.set
-- @see settings.clear
function unset(name)
    expect(1, name, "string")
    set_value(name, nil)
end

--- Resets the value of all settings. Equivalent to calling [`settings.unset`]
-- on every setting.
--
-- @see settings.unset
function clear()
    for name in pairs(values) do
        set_value(name, nil)
    end
end

--- Get the names of all currently defined settings.
--
-- @treturn { string } An alphabetically sorted list of all currently-defined
-- settings.
function getNames()
    local result, n = {}, 1
    for k in pairs(details) do
        result[n], n = k, n + 1
    end
    for k in pairs(values) do
        if not details[k] then result[n], n = k, n + 1 end
    end
    table.sort(result)
    return result
end

--- Load settings from the given file.
--
-- Existing settings will be merged with any pre-existing ones. Conflicting
-- entries will be overwritten, but any others will be preserved.
--
-- @tparam[opt=".settings"] string path The file to load from.
-- @treturn boolean Whether settings were successfully read from this
-- file. Reasons for failure may include the file not existing or being
-- corrupted.
--
-- @see settings.save
-- @changed 1.87.0 `path` is now optional.
function load(path)
    expect(1, path, "string", "nil")
    local file = fs.open(path or ".settings", "r")
    if not file then
        return false
    end

    local sText = file.readAll()
    file.close()

    local tFile = textutils.unserialize(sText)
    if type(tFile) ~= "table" then
        return false
    end

    for k, v in pairs(tFile) do
        local ty_v = type(v)
        if type(k) == "string" and (ty_v == "string" or ty_v == "number" or ty_v == "boolean" or ty_v == "table") then
            local opt = details[k]
            if not opt or not opt.type or ty_v == opt.type then
                -- This may fail if the table is recursive (or otherwise cannot be serialized).
                local ok, v = pcall(reserialize, v)
                if ok then set_value(k, v) end
            end
        end
    end

    return true
end

--- Save settings to the given file.
--
-- This will entirely overwrite the pre-existing file. Settings defined in the
-- file, but not currently loaded will be removed.
--
-- @tparam[opt=".settings"] string path The path to save settings to.
-- @treturn boolean If the settings were successfully saved.
--
-- @see settings.load
-- @changed 1.87.0 `path` is now optional.
function save(path)
    expect(1, path, "string", "nil")
    local file = fs.open(path or ".settings", "w")
    if not file then
        return false
    end

    file.write(textutils.serialize(values))
    file.close()

    return true
end
