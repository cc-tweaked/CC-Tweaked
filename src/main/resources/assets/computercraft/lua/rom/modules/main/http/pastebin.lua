--- Parse the pastebin code from the given code or URL
local function parseCode(paste)
	local patterns = {
		"^([%a%d]+)$",
		"^https?://pastebin.com/([%a%d]+)$",
		"^pastebin.com/([%a%d]+)$",
		"^https?://pastebin.com/raw/([%a%d]+)$",
		"^pastebin.com/raw/([%a%d]+)$",
	}

	for i = 1, #patterns do
		local code = paste:match(patterns[i])
		if code then
		return code
		end
	end

	return nil
end

-- Download the contents of a paste
local function download(code)
	if type(code) ~= "string" then
		error("bad argument #1 (expected string, got " .. type(code) .. ")", 2)
	end

	if not http then
		return false, "Pastebin requires http API"
	end

	-- Add a cache buster so that spam protection is re-checked
	local cacheBuster = ("%x"):format(math.random(0, 2 ^ 30))
	local response, err = http.get(
		"https://pastebin.com/raw/" .. textutils.urlEncode(code) .. "?cb=" .. cacheBuster
	)

	if not response then
		return response, err
	end

	-- If spam protection is activated, we get redirected to /paste with Content-Type: text/html
	local headers = response.getResponseHeaders()
	if not headers["Content-Type"] or not headers["Content-Type"]:find("^text/plain") then
		return false, "Pastebin blocked due to spam protection"
	end

	local contents = response.readAll()
	response.close()
	return contents
end

-- Upload text to pastebin
local function upload(name, text)
	if not http then
		return false, "Pastebin requires http API"
	end

	-- POST the contents to pastebin
	local key = "0ec2eb25b6166c0c27a394ae118ad829"
	local response = http.post(
		"https://pastebin.com/api/api_post.php",
		"api_option=paste&" ..
		"api_dev_key=" .. key .. "&" ..
		"api_paste_format=lua&" ..
		"api_paste_name=" .. textutils.urlEncode(name) .. "&" ..
		"api_paste_code=" .. textutils.urlEncode(text)
	)

	if not response then
		return false, "Failed."
	end

	local contents = response.readAll()
	response.close()

	return string.match(contents, "[^/]+$")
end

-- Download the contents to a file from pastebin
local function get(code, path)
	if type(code) ~= "string" then
		error( "bad argument #1 (expected string, got " .. type(code) .. ")", 2)
	end

	if type(path) ~= "string" then
		error("bad argument #2 (expected string, got " .. type(path) .. ")", 2)
	end

	local res, msg = download(code)
	if not res then
		return res, msg
	end

	local file = fs.open(path, "w")
	file.write(res)
	file.close()

	return true
end

-- Upload a file to pastebin.com
local function put(path)
	if type(path) ~= "string" then
		error("bad argument #1 (expected string, got " .. type(path) .. ")", 2)
	end

	-- Determine file to upload
	if not fs.exists(path) or fs.isDir(path) then
		return false, "No such file"
	end

	-- Read in the file
	local name = fs.getName(path)
	local file = fs.open(path, "r")
	local text = file.readAll()
	file.close()

	return upload(name, text)
end

return {
	download = download,
	upload = upload,
	get = get,
	put = put,
	parseCode = parseCode,
}

