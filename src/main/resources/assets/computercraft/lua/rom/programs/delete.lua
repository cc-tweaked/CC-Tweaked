local args = table.pack(...)

if args.n < 1 then
    print("Usage: rm <paths>")
    return
end

for i = 1, args.n do
    local files = fs.find(shell.resolve(args[i]))
    if #files > 0 then
        for n, file in ipairs(files) do
            fs.delete(file)
        end
    else
        printError(args[i] .. ": No matching files")
    end
end
