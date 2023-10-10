-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- Find help files on the current computer.
--
-- @module help
-- @since 1.2

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

--- Sets the colon-separated list of directories where help files are searched
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

local extensions = { "", ".md", ".txt" }

--- Returns the location of the help file for the given topic.
--
-- @tparam string topic The topic to find
-- @treturn string|nil The path to the given topic's help file, or `nil` if it
-- cannot be found.
-- @usage help.lookup("disk")
-- @changed 1.80pr1 Now supports finding .txt files.
-- @changed 1.97.0 Now supports finding Markdown files.
function lookup(topic)
    expect(1, topic, "string")
    -- Look on the path variable
    for path in string.gmatch(sPath, "[^:]+") do
        path = fs.combine(path, topic)
        for _, extension in ipairs(extensions) do
            local file = path .. extension
            if fs.exists(file) and not fs.isDir(file) then
                return file
            end
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
                        for i = 2, #extensions do
                            local extension = extensions[i]
                            if #sFile > #extension and sFile:sub(-#extension) == extension then
                                sFile = sFile:sub(1, -#extension - 1)
                            end
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
-- @since 1.74
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
