local args = table.pack(...)

if args.n < 1 then
    print("Usage: rm <paths>")
    return
end

for i = 1, args.n do
    local resolvedPath = shell.resolve(args[i])
    local files = fs.find(resolvedPath)
    if #files > 0 then
        for _, file in ipairs(files) do
            if file ~= resolvedPath and fs.isReadOnly(file) then
                printError("Cannot delete read-only file " .. file)
            elseif fs.isMountPoint(file) then
                print("Skipping seperate mount " .. file)
                if fs.isDir(file) then
                    print("To delete its contents run rm /" .. fs.combine(file, "*"))
                end
            else
                local ok, err = pcall(fs.delete, file)
                if not ok then
                    printError((err:gsub("^pcall: ", "")))
                end
            end
        end
    else
        printError(args[i] .. ": No matching files")
    end
end
