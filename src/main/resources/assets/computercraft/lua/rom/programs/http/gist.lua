-- gist.lua - Gist client for ComputerCraft
-- Made by JackMacWindows for CraftOS-PC and CC: Tweaked

if not http then
    printError("Gist requires http API")
    if _G.config ~= nil then printError("Set http_enable to true in the CraftOS-PC configuration")
    else printError("Set http_enable to true in ComputerCraft's configuration") end
    return 2
end

local gist = require "cc.http.gist"

local args = { ... }

local function readFile(filename, files, isEditing)
    if fs.isDir(shell.resolve(filename)) then
        for _, v in ipairs(fs.list(shell.resolve(filename))) do if readFile(fs.combine(filename, v), files, isEditing) then return true end end
    else
        if files[fs.getName(filename)] then print("Cannot upload files with duplicate names.") return true end
        local file = fs.open(shell.resolve(filename), "rb")
        if file == nil then
            if not isEditing then print("Could not read " .. filename .. ".") return true
            else files[fs.getName(filename)] = textutils.json_null end
        else
            files[fs.getName(filename)] = file.readAll()
            file.close()
        end
    end
end

local function getFiles(isEditing)
    local files = {}
    local i = isEditing and 3 or 2
    while args[i] ~= nil and args[i] ~= "--" do
        if readFile(args[i], files, isEditing) then return nil end
        i = i + 1
    end
    if args[i] == "--" then return files, table.concat({ table.unpack(args, i + 1) }, " ") end
    return files
end

local function setTextColor(c) if term.isColor() then term.setTextColor(c) elseif c == colors.white or c == colors.yellow then term.setTextColor(colors.white) else term.setTextColor(colors.lightGray) end end

local helpstr = "Usages:\ngist put <files...> [-- description...]\ngist edit <id> <files...> [-- description]\ngist delete <id>\ngist get <id> <filename>\ngist run <id> [arguments...]\ngist info <id>"

if #args < 2 then
    print(helpstr)
    return 1
end

if args[1] == "get" then
    if #args < 3 then print(helpstr) return 1 end
    if args[3]:sub(#args[3]) == "/" or fs.isDir(shell.resolve(args[3])) then
        fs.makeDir(shell.resolve(args[3]))
        local files, err = gist.getAll(args[2], write)
        if files == nil then printError(err) return 3 end
        for k, v in pairs(files) do
            local file = fs.open(shell.resolve(fs.combine(args[3], k)), "wb")
            file.write(v)
            file.close()
        end
        print("Downloaded all files to " .. shell.resolve(args[3]))
    else
        local data, err = gist.get(args[2], write)
        if data == nil then printError(err) return 3 end
        local file = fs.open(shell.resolve(args[3]), "wb")
        file.write(data)
        file.close()
        print("Downloaded as " .. shell.resolve(args[3]))
    end
elseif args[1] == "run" then
    return gist.run(args[2], write, table.unpack(args, 3))
elseif args[1] == "put" then
    local files, description = getFiles(false)
    if files == nil then return end
    local id, html_url = gist.put(files, description, nil, true)
    if id ~= nil then print("Uploaded as " .. html_url .. "\nRun 'gist get " .. id .. "' to download anywhere")
    else printError(html_url) return 3 end
elseif args[1] == "info" then
    local tab, err = gist.info(args[2], write)
    if tab == nil then printError(err) return 3 end
    setTextColor(colors.yellow)
    write("Description: ")
    setTextColor(colors.white)
    print(tab.description)
    setTextColor(colors.yellow)
    write("Author: ")
    setTextColor(colors.white)
    print(tab.author)
    setTextColor(colors.yellow)
    write("Revisions: ")
    setTextColor(colors.white)
    print(tab.revisionCount)
    setTextColor(colors.yellow)
    print("Files in this Gist:")
    setTextColor(colors.white)
    textutils.tabulate(tab.files)
elseif args[1] == "edit" then
    if #args < 3 then print(helpstr) return 1 end
    local files, description = getFiles(true)
    if files == nil then return 2 end
    if not description then description = gist.info(args[2], write).description end
    local id, html_url = gist.put(files, description, args[2], true)
    if id then print("Uploaded as " .. html_url .. "\nRun 'gist get " .. args[2] .. "' to download anywhere")
    else printError(html_url) return 3 end
elseif args[1] == "delete" then
    local ok, err = gist.delete(args[2], true)
    if ok then print("The requested Gist has been deleted.") else printError(err) return 3 end
else print(helpstr) return 1 end
