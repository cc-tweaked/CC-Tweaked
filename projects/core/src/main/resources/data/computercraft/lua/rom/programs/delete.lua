-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local args = table.pack(...)

if args.n < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <paths>")
    return
end

for i = 1, args.n do
    local files = fs.find(shell.resolve(args[i]))
    if #files > 0 then
        for _, file in ipairs(files) do
            if fs.isReadOnly(file) then
                printError("Cannot delete read-only file /" .. file)
            elseif fs.isDriveRoot(file) then
                printError("Cannot delete mount /" .. file)
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
