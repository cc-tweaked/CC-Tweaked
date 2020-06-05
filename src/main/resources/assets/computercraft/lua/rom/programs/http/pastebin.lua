local function printUsage()
    print("Usages:")
    print("pastebin put <filename>")
    print("pastebin get <code> <filename>")
    print("pastebin run <code> <arguments>")
    print("pastebin connect")
    print("pastebin disconnect")
    print("pastebin list")
    print("pastebin infos")
    print("pastebin delete <code>")
end

local tArgs = { ... }
if #tArgs < 1 then
    printUsage()
    return
end

if not http then
    printError("Pastebin requires the http API")
    printError("Set http.enabled to true in CC: Tweaked's config")
    return
end

--- Attempts to guess the pastebin ID from the given code or URL
local function extractId(paste)
    local patterns = {
        "^([%a%d]+)$",
        "^https?://pastebin.com/([%a%d]+)$",
        "^pastebin.com/([%a%d]+)$",
        "^https?://pastebin.com/raw/([%a%d]+)$",
        "^pastebin.com/raw/([%a%d]+)$",
    }

    for i = 1, #patterns do
        local code = paste:match(patterns[i])
        if code then return code end
    end

    return nil
end

local function get(url)
    local paste = extractId(url)
    if not paste then
        io.stderr:write("Invalid pastebin code.\n")
        io.write("The code is the ID at the end of the pastebin.com URL.\n")
        return
    end

    write("Connecting to pastebin.com... ")
    -- Add a cache buster so that spam protection is re-checked
    local cacheBuster = ("%x"):format(math.random(0, 2 ^ 30))
    local response, err = http.get(
        "https://pastebin.com/raw/" .. textutils.urlEncode(paste) .. "?cb=" .. cacheBuster
    )

    if response then
        -- If spam protection is activated, we get redirected to /paste with Content-Type: text/html
        local headers = response.getResponseHeaders()
        if not headers["Content-Type"] or not headers["Content-Type"]:find("^text/plain") then
            io.stderr:write("Failed.\n")
            print("Pastebin blocked the download due to spam protection. Please complete the captcha in a web browser: https://pastebin.com/" .. textutils.urlEncode(paste))
            return
        end

        print("Success.")

        local sResponse = response.readAll()
        response.close()
        return sResponse
    else
        io.stderr:write("Failed.\n")
        print(err)
    end
end

local function print_parsed_list(input)
    local sCodes = string.gmatch(input, "<paste_key>([^\<]+)</paste_key>")
    local sNames = string.gmatch(input, "<paste_title>([^\<]+)</paste_title>")
    print("code     | name")
    local code = sCodes()
    local name = sNames()
    while code ~= nil and name ~= nil do
        print(code.." | "..name)
        code = sCodes()
        name = sNames()
    end
end

local sCommand = tArgs[1]

local dev_key = "0ec2eb25b6166c0c27a394ae118ad829"

if sCommand == "connect" then
    write("Username: ")
    username = read()
    write("Password: ")
    password = read("*")

    write("Connecting to pastebin.com... ")
    local response = http.post(
    "https://pastebin.com/api/api_login.php",
    "api_dev_key=" .. dev_key .. "&" ..
    "api_user_name=" .. textutils.urlEncode(username) .. "&" ..
    "api_user_password=" .. textutils.urlEncode(password)
    )
    if response then
        print("Success.")

        local sResponse = response.readAll()
        response.close()

        print(sResponse)
        settings.set("pastebin.key", sResponse)
        settings.save(".settings")
    else
        print("failed")
    end

elseif sCommand == "disconnect" then
    settings.unset("pastebin.key")
    settings.save(".settings")
    print("Disconnected")

elseif sCommand == "list" then
    local user_key = settings.get("pastebin.key")
    if not user_key then
        io.stderr:write("You aren't logged")
        print("first use: pastebin connect")
        return
    end

    write("Connecting to pastebin.com... ")
    local response = http.post(
    "https://pastebin.com/api/api_post.php",
    "api_option=list&" ..
    "api_dev_key=" .. dev_key .. "&" ..
    "api_user_key=" .. user_key
    )

    if response then
        local sResponse = response.readAll()
        response.close()
        if sResponse == "Bad API request, invalid api_user_key" then
            print("Failed.")
            print("You aren't logged")
            print("first use: pastebin connect")
        elseif sResponse == "No pastes found." then
            print("Success.")
            print("No pastes found.")
        else
            print("Success.")
            print_parsed_list(sResponse)
        end
    else
        print("Failed.")
    end

elseif sCommand == "infos" then
    local user_key = settings.get("pastebin.key")
    if not user_key then
        io.stderr:write("You aren't logged")
        print("first use: pastebin connect")
        return
    end

    write("Connecting to pastebin.com... ")
    local response = http.post(
    "https://pastebin.com/api/api_post.php",
    "api_option=userdetails&" ..
    "api_dev_key=" .. dev_key .. "&" ..
    "api_user_key=" .. user_key
    )

    if response then
        local sResponse = response.readAll()
        response.close()
        if sResponse == "Bad API request, invalid api_user_key" then
            print("Failed.")
            print("You aren't logged")
            print("first use: pastebin connect")
        else
            print("Success.")
            local sValues = string.gmatch(sResponse, "<user_([^\>]+)>([^\<]+)<")
            local fields, value = sValues()
            while fields ~= nil do
                print(fields..": "..value)
                fields, value = sValues()
            end
        end
    else
        print("Failed.")
    end

elseif sCommand == "delete" then
    if #tArgs < 2 then
        printUsage()
        return
    end

    local user_key = settings.get("pastebin.key")
    if not user_key then
        io.stderr:write("You aren't logged")
        print("first use: pastebin connect")
        return
    end

    local paste_key = extractId(tArgs[2])
    if not paste_key then
        io.stderr:write("Invalid pastebin code.\n")
        io.write("The code is the ID at the end of the pastebin.com URL.\n")
        return
    end

    write("Connecting to pastebin.com... ")
    local response = http.post(
    "https://pastebin.com/api/api_post.php",
    "api_option=delete&" ..
    "api_dev_key=" .. dev_key .. "&" ..
    "api_user_key=" .. user_key .. "&" ..
    "api_paste_key=" ..paste_key
    )

    if response then
        print("Success.")

        local sResponse = response.readAll()
        response.close()

        print(sResponse)
    else
        print("Failed.")
    end

elseif sCommand == "put" then
    if #tArgs < 2 then
        printUsage()
        return
    end
    -- Upload a file to pastebin.com
    -- Determine file to upload
    local user_key = settings.get("pastebin.key")
    local sFile = tArgs[2]
    local sPath = shell.resolve(sFile)
    if not fs.exists(sPath) or fs.isDir(sPath) then
        print("No such file")
        return
    end

    -- Read in the file
    local sName = fs.getName(sPath)
    local file = fs.open(sPath, "r")
    local sText = file.readAll()
    file.close()

    -- POST the contents to pastebin
    write("Connecting to pastebin.com... ")
    local params = "api_option=paste&" ..
    "api_dev_key=" .. dev_key .. "&" ..
    "api_paste_format=lua&" ..
    "api_paste_name=" .. textutils.urlEncode(sName) .. "&" ..
    "api_paste_code=" .. textutils.urlEncode(sText)
    if user_key ~= nil then
        params = params .. "&" .. "api_user_key=" .. user_key
        write("Connected")
    end
    local response = http.post(
        "https://pastebin.com/api/api_post.php",
        params)

    if response then
        print("Success.")

        local sResponse = response.readAll()
        response.close()

        local sCode = string.match(sResponse, "[^/]+$")
        print("Uploaded as " .. sResponse)
        print("Run \"pastebin get " .. sCode .. "\" to download anywhere")

    else
        print("Failed.")
    end

elseif sCommand == "get" then
    -- Download a file from pastebin.com
    if #tArgs < 3 then
        printUsage()
        return
    end

    -- Determine file to download
    local sCode = tArgs[2]
    local sFile = tArgs[3]
    local sPath = shell.resolve(sFile)
    if fs.exists(sPath) then
        print("File already exists")
        return
    end

    -- GET the contents from pastebin
    local res = get(sCode)
    if res then
        local file = fs.open(sPath, "w")
        file.write(res)
        file.close()

        print("Downloaded as " .. sFile)
    end
elseif sCommand == "run" then
    local sCode = tArgs[2]

    local res = get(sCode)
    if res then
        local func, err = load(res, sCode, "t", _ENV)
        if not func then
            printError(err)
            return
        end
        local success, msg = pcall(func, select(3, ...))
        if not success then
            printError(msg)
        end
    end
else
    printUsage()
    return
end
