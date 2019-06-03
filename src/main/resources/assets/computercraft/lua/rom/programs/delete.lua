local args = table.pack(...)

if args.n < 1 then
    print("Usage: rm <paths>")
    return
end

for i = 1, args.n do
    local files = fs.find(shell.resolve(args[i]))
    if #files > 0 then
        for n, file in ipairs(files) do
            if fs.isReadOnly( file ) or file == "" then
                printError( "/" .. file .. ": Access denied")
            else
                fs.delete(file)
            end
        end
    else
        printError(args[i] .. ": No matching files")
    end
end
