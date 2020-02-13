
local tArgs = { ... }

-- Get all the files in the directory
local sDir
if #tArgs == 0 then
    sDir = shell.dir()
elseif string.sub(tArgs[1],1,1) == "-" then
    sArgs = string.sub(tArgs[1],2,-1)
    for c in sArgs:gmatch"." do
        if c == "a" then
            bShowHiddenArg = true
        elseif c == "l" then
            bShowAttr = true
        else
            printError("Unknown Argument")
            return
        end
    end
    if tArgs[2] == nil then
        sDir = shell.dir()
    else
        sDir = shell.resolve( tArgs[1] )
    end
else
    sDir = shell.resolve( tArgs[1] )
end

if not fs.isDir( sDir ) then
    printError( "Not a directory" )
    return
end

-- Sort into dirs/files, and calculate column count
local tAll = fs.list( sDir )
local tFiles = {}
local tDirs = {}

local bShowHidden = settings.get( "list.show_hidden" ) or bShowHiddenArg
for _, sItem in pairs( tAll ) do
    if bShowHidden or string.sub( sItem, 1, 1 ) ~= "." then
        local sPath = fs.combine( sDir, sItem )
        if fs.isDir( sPath ) then
            table.insert( tDirs, sItem )
        else
            table.insert( tFiles, sItem )
        end
    end
end
table.sort( tDirs )
table.sort( tFiles )

local function writeFileTable( tPrint, tList )
    for i, v in ipairs( tList ) do
        tAttr = fs.attributes( shell.resolve( v ) )
        tSingeEntry = {v}
        if tAttr["isDir"] then
            table.insert( tSingeEntry, "Dir" )
        else
            table.insert( tSingeEntry, "File" )
        end
        sDate = settings.get("list.date_format")
        table.insert( tSingeEntry, tostring( tAttr["size"] ) )
        table.insert( tSingeEntry, os.date( sDate, tAttr["created"] ) )
        table.insert( tSingeEntry, os.date( sDate, tAttr["modification"] ) )
        table.insert( tPrint, tSingeEntry )
    end
    return tPrint
end

if bShowAttr then
    tPrint = {{"Name","Type","Size","Created","Modified"}}
    tPrint = writeFileTable( tPrint, tDirs )
    tPrint = writeFileTable( tPrint, tFiles )
    textutils.tabulate( table.unpack( tPrint ) )
else
    if term.isColour() then
        textutils.pagedTabulate( colors.green, tDirs, colors.white, tFiles )
    else
        textutils.pagedTabulate( tDirs, tFiles )
    end
end
