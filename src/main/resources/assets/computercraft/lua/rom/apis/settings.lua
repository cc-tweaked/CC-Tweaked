--- The settings API allows to store values and save them to a file for
-- persistent configurations for CraftOS and your programs.
--
-- By default, the settings API will load its configuration from the
-- `/.settings` file. One can then use @{settings.save} to update the file.
--
-- @module settings

local expect = dofile("rom/modules/main/cc/expect.lua").expect

local tSettings = {}

--- Set the value of a setting.
--
-- @tparam string name The name of the setting to set
-- @param value The setting's value. This cannot be `nil`, and must be
-- serialisable by @{textutils.serialize}.
-- @throws If this value cannot be serialised
-- @see settings.unset
function set( name, value )
    expect(1, name, "string")
    expect(2, value, "number", "string", "boolean", "table")

    if type(value) == "table" then
        -- Ensure value is serializeable
        value = textutils.unserialize( textutils.serialize(value) )
    end
    tSettings[ name ] = value
end

local copy
function copy( value )
    if type(value) == "table" then
        local result = {}
        for k, v in pairs(value) do
            result[k] = copy(v)
        end
        return result
    else
        return value
    end
end

--- Get the value of a setting.
--
-- @tparam string name The name of the setting to get.
-- @param[opt] default The value to use should there be pre-existing value for
-- this setting. Defaults to `nil`.
-- @return The setting's, or `default` if the setting has not been set.
function get( name, default )
    expect(1, name, "string")
    local result = tSettings[ name ]
    if result ~= nil then
        return copy(result)
    else
        return default
    end
end

--- Remove the value of a setting, clearing it back to `nil`.
--
-- @{settings.get} will return the default value until the setting's value is
-- @{settings.set|set}, or the computer is rebooted.
--
-- @tparam string name The name of the setting to unset.
-- @see settings.set
-- @see settings.clear
function unset( name )
    expect(1, name, "string")
    tSettings[ name ] = nil
end

--- Removes the value of all settings. Equivalent to calling @{settings.unset}
--- on every setting.
--
-- @see settings.unset
function clear()
    tSettings = {}
end

--- Get the names of all currently defined settings.
--
-- @treturn { string } An alphabetically sorted list of all currently-defined
-- settings.
function getNames()
    local result = {}
    for k in pairs( tSettings ) do
        result[ #result + 1 ] = k
    end
    table.sort(result)
    return result
end

--- Load settings from the given file.
--
-- Existing settings will be merged with any pre-existing ones. Conflicting
-- entries will be overwritten, but any others will be preserved.
--
-- @tparam string sPath The file to load from.
-- @treturn boolean Whether settings were successfully read from this
-- file. Reasons for failure may include the file not existing or being
-- corrupted.
--
-- @see settings.save
function load( sPath )
    expect(1, sPath, "string")
    local file = fs.open( sPath, "r" )
    if not file then
        return false
    end

    local sText = file.readAll()
    file.close()

    local tFile = textutils.unserialize( sText )
    if type(tFile) ~= "table" then
        return false
    end

    for k, v in pairs(tFile) do
        if type(k) == "string" and
           (type(v) == "string" or type(v) == "number" or type(v) == "boolean" or type(v) == "table") then
            set( k, v )
        end
    end

    return true
end

--- Save settings to the given file.
--
-- This will entirely overwrite the pre-existing file. Settings defined in the
-- file, but not currently loaded will be removed.
--
-- @tparam string sPath The path to save settings to.
-- @treturn boolean If the settings were successfully saved.
--
-- @see settings.load
function save( sPath )
    expect(1, sPath, "string")
    local file = fs.open( sPath, "w" )
    if not file then
        return false
    end

    file.write( textutils.serialize( tSettings ) )
    file.close()

    return true
end
