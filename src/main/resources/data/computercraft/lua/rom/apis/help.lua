--- Provides an API to read help files.
--
-- @module help

local expect = dofile("rom/modules/main/cc/expect.lua").expect

local sPath = "/rom/help"

--- Returns a colon-separated list of directories where help files are searched
-- for. All directories are absolute.
--
-- @treturn string The current help search path, separated by colons.
-- @see help.setPath
function path()
    return sPath
end

--- Sets the colon-seperated list of directories where help files are searched
-- for to `newPath`
--
-- @tparam string newPath The new path to use.
-- @usage help.setPath( "/disk/help/" )
-- @usage help.setPath( help.path() .. ":/myfolder/help/" )
-- @see help.path
function setPath(_sPath)
    expect(1, _sPath, "string")
    sPath = _sPath
end

--- Returns the location of the help file for the given topic.
--
-- @tparam string topic The topic to find
-- @treturn string|nil The path to the given topic's help file, or `nil` if it
-- cannot be found.
-- @usage help.lookup("disk")
function lookup(_sTopic)
    expect(1, _sTopic, "string")
    -- Look on the path variable
    for sPath in string.gmatch(sPath, "[^:]+") do
        sPath = fs.combine(sPath, _sTopic)
        if fs.exists(sPath) and not fs.isDir(sPath) then
            return sPath
        elseif fs.exists(sPath .. ".txt") and not fs.isDir(sPath .. ".txt") then
            return sPath .. ".txt"
        end
    end

    -- Not found
    return nil
end

--- Returns a list of topics that can be looked up and/or displayed.
--
-- @treturn table A list of topics in alphabetical order.
-- @usage help.topics()
function topics()
    -- Add index
    local tItems = {
        ["index"] = true,
    }

    -- Add topics from the path
    for sPath in string.gmatch(sPath, "[^:]+") do
        if fs.isDir(sPath) then
            local tList = fs.list(sPath)
            for _, sFile in pairs(tList) do
                if string.sub(sFile, 1, 1) ~= "." then
                    if not fs.isDir(fs.combine(sPath, sFile)) then
                        if #sFile > 4 and sFile:sub(-4) == ".txt" then
                            sFile = sFile:sub(1, -5)
                        end
                        tItems[sFile] = true
                    end
                end
            end
        end
    end

    -- Sort and return
    local tItemList = {}
    for sItem in pairs(tItems) do
        table.insert(tItemList, sItem)
    end
    table.sort(tItemList)
    return tItemList
end

--- Returns a list of topic endings that match the prefix. Can be used with
-- `read` to allow input of a help topic.
--
-- @tparam string prefix The prefix to match
-- @treturn table A list of matching topics.
function completeTopic(sText)
    expect(1, sText, "string")
    local tTopics = topics()
    local tResults = {}
    for n = 1, #tTopics do
        local sTopic = tTopics[n]
        if #sTopic > #sText and string.sub(sTopic, 1, #sText) == sText then
            table.insert(tResults, string.sub(sTopic, #sText + 1))
        end
    end
    return tResults
end
