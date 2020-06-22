local expect = require("cc.expect").expect

local programLanguage = nil
local translations = {}

local function loadFile(lang, path)
    expect(1, lang, "string")
    expect(2, path, "string")
    if not fs.exists(path) or fs.isDir(path) then
        return false
    end
    if translations[lang] == nil then
        translations[lang] = {}
    end
    local file = fs.open(path, "r")
    local data, err = textutils.unserialiseJSON(file.readAll())
    file.close()
    if not data then
        return false, err
    end
    for key, value in pairs(data) do
        translations[lang][key] = value
    end
    return true
end

local function loadDirectory(path)
    expect(1, path, "string")
    local success = true
    local files = fs.list(path)
    for _, i in ipairs(files) do
        local lang = fs.getName(i):sub(1, -6)
        local ok = loadFile(lang, fs.combine(path, i))
        if not ok then
            success = false
        end
    end
    return success
end

local function loadTable(lang, ta)
    expect(1, lang, "string")
    expect(2, ta, "table")
    if translations[lang] == nil then
        translations[lang] = {}
    end
    for key, value in pairs(ta) do
        translations[lang][key] = value
    end
end

local function setKey(lang, key, value)
    expect(1, lang, "string")
    expect(2, key, "string")
    expect(3, value, "string")
    if translations[lang] == nil then
        translations[lang] = {}
    end
    translations[lang][key] = value
end

local function listKeys()
    local keylist = {}
    for key in pairs(translations.en_us) do
        table.insert(keylist, key)
    end
    return keylist
end

local function listLanguages()
    local langlist = {}
    for key in pairs(translations) do
        table.insert(langlist, key)
    end
    return langlist
end

local function setLanguage(lang)
    expect(1, lang, "string")
    programLanguage = lang
end

local function translate(key, default, language)
    expect(1, key, "string")
    local lang = language or programLanguage or settings.get("language")
    local defaultLang = "en_us"
    if translations[lang] == nil then
        if translations[defaultLang][key] == nil then
            return default or key
        else
            return translations[defaultLang][key]
        end
    end
    if translations[lang][key] == nil then
        if translations[defaultLang][key] == nil then
            return default or key
        else
            return translations[defaultLang][key]
        end
    else
        return translations[lang][key]
    end
end

loadDirectory("/rom/lang")

return {
    loadFile = loadFile,
    loadDirectory = loadDirectory,
    loadTable = loadTable,
    setKey = setKey,
    listKeys = listKeys,
    listLanguages = listLanguages,
    setLanguage = setLanguage,
    translate = translate,
}
