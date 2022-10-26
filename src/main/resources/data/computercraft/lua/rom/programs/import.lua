local completion = require "cc.completion"

print("Drop files to transfer them to this computer")

local files
while true do
    local event, arg = os.pullEvent()
    if event == "file_transfer" then
        files = arg.getFiles()
        break
    elseif event == "key" and arg == keys.q then
        return
    end
end

if #files == 0 then
  print("No files to transfer")
end

local overwrite = {}
for _, file in pairs(files) do
    local filename = file.getName()
    local path = shell.resolve(filename)
    if fs.exists(path) then
        if fs.isDir(path) then
            printError(filename .. " is already a directory.")
            return
        end

        overwrite[#overwrite + 1] = filename
    end
end

if #overwrite > 0 then
    table.sort(overwrite)
    printError("The following files will be overwritten:")
    textutils.pagedTabulate(overwrite)

    while true do
        io.write("Overwrite? (yes/no) ")
        local input = read(nil, nil, function(t)
            return completion.choice(t, { "yes", "no" })
        end)
        if not input then return end

        input = input:lower()
        if input == "" or input == "yes" or input == "y" then
            break
        elseif input == "no" or input == "n" then
            return
       end
   end
end

for _, file in pairs(files) do
    local filename = file.getName()
    print("Transferring " .. filename)

    local path = shell.resolve(filename)
    local handle, err = fs.open(path, "wb")
    if not handle then printError(err) return end

    handle.write(file.readAll())
    handle.close()
end
