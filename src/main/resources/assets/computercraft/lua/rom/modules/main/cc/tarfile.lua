local function to_decimal(s)
  local decimal = 0
  for i = 1, #s do
    local digit = tonumber(s:sub(i, i))
    if not digit or digit >= 8 then return 0 end
    decimal = decimal + digit * 8 ^ (#s - i)
  end
  return decimal
end

local function open_read(sPath)
    local tHandle = setmetatable({},{})
    tHandle.tBlocks = {}
    tHandle.tFiles = {}
    local f = fs.open(sPath,"rb")
    local nCount = 1
    while true do
        block = f.read(512)
        if block == nil then
            f.close()
            break
        end
        table.insert(tHandle.tBlocks,block)
        local nPos = block:find("\000")
        local sFilename = block:sub(1,nPos)
        if sFilename ~= "\000" then
            sTypebyte = block:sub(157,157)
            if sTypebyte == "0" then
                local nStartBlock = nCount
                local sSizeBytes = block:sub(125,135)
                local nFileSize = to_decimal(sSizeBytes)
                local nRestSize = nFileSize
                while nRestSize > 512 do
                    table.insert(tHandle.tBlocks,f.read(512))
                    nRestSize = nRestSize -512
                    nCount = nCount + 1
                end
                table.insert(tHandle.tFiles,{name=sFilename,startblock=nStartBlock,endblock=nCount,type="file",size=nFileSize,restsize=nRestSize})
            elseif sTypebyte == "5" then
                table.insert(tHandle.tFiles,{name=sFilename,startblock=nCount,endblock=nCount,type="directory",size=0,restsize=0})
                nCount = nCount + 1
            end
        end
    end
    function tHandle.list(tHandle)
        local tList = {}
        for _,i in ipairs(tHandle.tFiles) do
            table.insert(tList,i["name"])
        end
        return tList
    end
    function tHandle.type(tHandle,sName)
        for _,i in ipairs(tHandle.tFiles) do
            if i["name"] == sName then
                return i["type"]
            end
        end
    end
    function tHandle.size(tHandle,sName)
        for _,i in ipairs(tHandle.tFiles) do
            if i["name"] == sName then
                return i["size"]
            end
        end
    end
    function tHandle.read(tHandle,sFile)
        for _,i in ipairs(tHandle.tFiles) do
            if i["name"] == sFile then
                sFilecontent = ""
                for n=i["startblock"]+1,i["endblock"]-1 do
                    sFilecontent = sFilecontent .. tHandle.tBlocks[n]
                end
                sFilecontent = sFilecontent .. tHandle.tBlocks[i["endblock"]]:sub(1,i["restsize"])
                return sFilecontent
            end
        end
    end
    function tHandle.extract(tHandle,sFile,sPath)
        sData = tHandle:read(sFile)
        f = fs.open(sPath,"wb")
        f.write(sData)
        f.close()
    end
    function tHandle.extractAll(tHandle,sPath)
        for _,i in ipairs(tHandle:list()) do
            local sExtractPath = fs.combine(sPath,i)
            if tHandle:type(i) == "file" then
                tHandle:extract(i,sExtractPath)
            elseif tHandle:type(i) == "directory" then
                fs.makeDir(sExtractPath)
            end
        end
    end
    return tHandle
end

local function open(sPath,sMode)
    if sMode == "r" then
        return open_read(sPath)
    else
        error("Unsupported mode",2)
    end
end

return {
    open=open
}
