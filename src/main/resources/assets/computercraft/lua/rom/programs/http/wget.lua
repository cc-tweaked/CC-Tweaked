local translate = require("cc.translate").translate

local function printUsage()
    print(translate("cc.wget.usage_title"))
    print("wget <url> " .. translate("cc.wget.usage_filename"))
    print("wget run <url>")
end

local tArgs = { ... }

local run = false
if tArgs[1] == "run" then
    table.remove(tArgs, 1)
    run = true
end

if #tArgs < 1 then
    printUsage()
    return
end

local url = table.remove(tArgs, 1)

if not http then
    printError(translate("cc.wget.no_http"))
    return
end

local function getFilename(sUrl)
    sUrl = sUrl:gsub("[#?].*" , ""):gsub("/+$" , "")
    return sUrl:match("/([^/]+)$")
end

local function get(sUrl)
    -- Check if the URL is valid
    local ok, err = http.checkURL(url)
    if not ok then
        printError(err or translate("cc.wget.invalid_url"))
        return
    end

    write(translate("cc.wget.connecting"):format(sUrl))

    local response = http.get(sUrl , nil , true)
    if not response then
        print(translate("cc.wget.failed"))
        return nil
    end

    print(translate("cc.wget.success"))

    local sResponse = response.readAll()
    response.close()
    return sResponse
end

if run then
    local res = get(url)
    if not res then return end

    local func, err = load(res, getFilename(url), "t", _ENV)
    if not func then
        printError(err)
        return
    end

    local ok, err = pcall(func, table.unpack(tArgs))
    if not ok then
        printError(err)
    end
else
    local sFile = tArgs[1] or getFilename(url)
    local sPath = shell.resolve(sFile)
    if fs.exists(sPath) then
        print(translate("cc.wget.exists"))
        return
    end

    local res = get(url)
    if not res then return end

    local file = fs.open(sPath, "wb")
    file.write(res)
    file.close()

    print(translate("cc.wget.downloaded"):format(sFile))
end
