local args = table.pack(...)

if args.n < 1 then
    print("Usage: rm <paths>")
    return
end

for i = 1, args.n do
    local files = fs.find(shell.resolve(args[i]))
    if #files > 0 then
        for _, file in ipairs(files) do
            local ok, err = pcall(fs.delete, file)
            if not ok then
                printError((err:gsub("^pcall: ", "")))
            end
        end
    else
        printError(args[i] .. ": No matching files")
    end
end
