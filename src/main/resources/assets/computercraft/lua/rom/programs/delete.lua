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
            if fs.isReadOnly(file) then
                print("Skipping read only "..file)
            elseif fs.getDrive(resolvedPath) ~= fs.getDrive(file) then
                print("Skipping seperate mount "..file)
                print("To delete it's contents run rm "..fs.combine(file, "*"))
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
