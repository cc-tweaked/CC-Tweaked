local tMotd = {}

for sPath in string.gmatch(settings.get("motd.path"), "[^:]+") do
    if fs.exists(sPath) then
        for sLine in io.lines(sPath) do
            table.insert(tMotd, sLine)
        end
    end
end

if #tMotd == 0 then
    print("missingno")
else
    print(tMotd[math.random(1, #tMotd)])
end
