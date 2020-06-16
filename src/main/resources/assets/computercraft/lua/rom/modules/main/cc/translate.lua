local translations = {}

local function loadFile(path)
    local langCode = fs.getName(path):sub(1,-6)
    if translations[langCode] == nil then
        translations[langCode] = {}
    end
    file = fs.open(path,"r")
    data = textutils.unserialiseJSON(file.readAll())
    file.close()
    for key, value in pairs(data) do
        translations[langCode][key] = value
    end
end

function loadDirectory(path)
    files = fs.list(path)
    for _,i in ipairs(files) do
        loadFile(fs.combine(path,i))
    end
end

function translate(key)
    lang = settings.get("language")
    defaultLang = "en_us"
    if translations[lang] == nil then
        if translations[defaultLang][key] == nil then
            return key
        else
            return translations[defaultLang][key]
        end
    end
    if translations[lang][key] == nil then
        if translations[defaultLang][key] == nil then
            return key
        else
            return translations[defaultLang][key]
        end
    else
        return translations[lang][key]
    end
end

loadDirectory("/rom/lang")

return {
    loadDirectory = loadDirectory,
    translate = translate
}
