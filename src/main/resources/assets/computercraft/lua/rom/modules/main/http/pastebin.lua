
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
    local code = paste:match( patterns[i] )
    if code then return code end
  end

  return nil
end

function download(url)
  if type( url ) ~= "string" then
    error( "bad argument #1 (expected string, got " .. type( url ) .. ")", 2 )
  end

  if not http then
    return false, "Pastebin requires http API"
  end

  local paste = extractId( url )
  if not paste then
    return false, "Invalid pastebin code. The code is the ID at the end of the pastebin.com URL."
  end

  -- Add a cache buster so that spam protection is re-checked
  local cacheBuster = ("%x"):format(math.random(0, 2^30))
  local response, err = http.get(
    "https://pastebin.com/raw/"..textutils.urlEncode( paste ).."?cb="..cacheBuster
  )

  if not response then
    return response, err
  end

  -- If spam protection is activated, we get redirected to /paste with Content-Type: text/html
  local headers = response.getResponseHeaders()
  if not headers["Content-Type"] or not headers["Content-Type"]:find( "^text/plain" ) then
    return false, "Pastebin blocked the download due to spam protection. Please complete the captcha in a web browser: https://pastebin.com/" .. textutils.urlEncode( paste )
  end

  local sResponse = response.readAll()
  response.close()
  return sResponse
end

local function put(sPath)
  if type( sPath ) ~= "string" then
    error( "bad argument #1 (expected string, got " .. type( sPath ) .. ")", 2 )
  end

  if not http then
    return false, "Pastebin requires http API"
  end

  -- Upload a file to pastebin.com
  -- Determine file to upload
  if not fs.exists( sPath ) or fs.isDir( sPath ) then
    return false, "No such file"
  end

  -- Read in the file
  local sName = fs.getName( sPath )
  local file = fs.open( sPath, "r" )
  local sText = file.readAll()
  file.close()

  -- POST the contents to pastebin
  local key = "0ec2eb25b6166c0c27a394ae118ad829"
  local response = http.post(
    "https://pastebin.com/api/api_post.php",
    "api_option=paste&"..
    "api_dev_key="..key.."&"..
    "api_paste_format=lua&"..
    "api_paste_name="..textutils.urlEncode(sName).."&"..
    "api_paste_code="..textutils.urlEncode(sText)
  )

  if response then
    local sResponse = response.readAll()
    response.close()

    return string.match( sResponse, "[^/]+$" )
  end
  return false, "Failed."
end

local function get(sCode, sPath)
  if type( sCode ) ~= "string" then
    error( "bad argument #1 (expected string, got " .. type( sCode ) .. ")", 2 )
  end

  if type( sPath ) ~= "string" then
    error( "bad argument #2 (expected string, got " .. type( sPath ) .. ")", 2 )
  end

  if not http then
    return false, "Pastebin requires http API"
  end

  if fs.exists( sPath ) then
    return false, "File already exists"
  end

  -- GET the contents from pastebin
  local res, msg = download(sCode)
  if not res then
    return res, msg
  end

  local file = fs.open( sPath, "w" )
  file.write( res )
  file.close()

  return sPath
end

local function run(sCode, ...)
    if not http then
        return false, "Pastebin requires http API"
    end

    if type( sCode ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sCode ) .. ")", 2 )
    end

    local res, msg = download(sCode)
    if not res then
        return res, msg
    end
    local func, err = load(res, sCode, "t", _ENV)
    if not func then
        return func, err
    end
    return pcall(func, ...)
end

return {
  get = get,
  put = put,
  run = run,
}