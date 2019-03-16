--- Generates impostor recipes and advancements for several dynamic recipes.
--
-- Namely:
-- - Turtle upgrades
-- - Pocket upgrades
-- - Disk (each colour)
--
-- Note, this is largely intended for the recipe book, as that requires a fixed
-- set of recipes.

--- All turtle upgrades, and an optional item for this upgrade.
local turtle_upgrades = {
    { "computercraft:wireless_modem", '"computercraft:peripheral", "data": 1' },
    { "computercraft:advanced_modem" },
    { "computercraft:speaker", '"computercraft:peripheral", "data": 5' },
    { "minecraft:crafting_table" },
    { "minecraft:diamond_sword" },
    { "minecraft:diamond_shovel" },
    { "minecraft:diamond_pickaxe" },
    { "minecraft:diamond_axe" },
    { "minecraft:diamond_hoe" },
}

--- All pocket upgrades, and an optional item for this upgrade.
local pocket_upgrades = {
    { "computercraft:wireless_modem", '"computercraft:peripheral", "data": 1' },
    { "computercraft:advanced_modem" },
    { "computercraft:speaker", '"computercraft:peripheral", "data": 5' },
}

--- All dye/disk colours
local colours = {
    0x111111,
    0xcc4c4c,
    0x57A64E,
    0x7f664c,
    0x3366cc,
    0xb266e5,
    0x4c99b2,
    0x999999,
    0x4c4c4c,
    0xf2b2cc,
    0x7fcc19,
    0xdede6c,
    0x99b2f2,
    0xe57fd8,
    0xf2b233,
    0xf0f0f0;
}

--- Read the provided file into a string, exiting the program if not found.
--
-- @tparam string file The file to read
-- @treturn string The file's contents
local function read_all(file)
    local h, e = io.open(file, "rb")
    if not h then
        io.stderr:write("Cannot open " .. file .. ": " .. tostring(e))
        os.exit(1)
    end

    local c = h:read "*a"
    h:close()
    return c
end

--- Write the provided string into a file, exiting on failure.
--
-- @tparam string file The file to read
-- @tparam string contents The new contents
local function write_all(file, contents)
    local h, e = io.open(file, "wb")
    if not h then
        io.stderr:write("Cannot open " .. file .. ": " .. tostring(e))
        os.exit(1)
    end

    h:write(contents)
    h:close()
end

--- Format template strings of the form `${key}` using the given substituion
-- table.
local function template(str, subs)
    return str:gsub("%$%{([^}]+)%}", function(k) return subs[k] or error("Unknown key " .. k) end)
end

-- Write turtle upgrades
local turtle_recipe = read_all "tools/turtle_upgrade_recipe.json"
local turtle_advance = read_all "tools/turtle_upgrade_advancement.json"
for _, turtle in ipairs { { "normal", "computercraft:turtle_expanded" }, { "advanced", "computercraft:turtle_advanced" } } do
    local turtle_family, turtle_item = turtle[1], turtle[2]
    for _, upgrade in ipairs(turtle_upgrades) do
        local upgrade_id, upgrade_item = upgrade[1], upgrade[2] or ('"%s"'):format(upgrade[1])
        local path = ("generated/turtle_%s/%s"):format(turtle_family, (upgrade_id:gsub(":", "_")))
        local keys = {
            upgrade_id = upgrade_id, upgrade_item = upgrade_item,
            turtle_family = turtle_family, turtle_item = turtle_item,
            path = path,
        }

        write_all("src/main/resources/assets/computercraft/recipes/" .. path .. ".json", template(turtle_recipe, keys))
        write_all("src/main/resources/assets/computercraft/advancements/recipes/" .. path .. ".json", template(turtle_advance, keys))
    end
end

-- Write pocket upgrades
local pocket_recipe = read_all "tools/pocket_upgrade_recipe.json"
local pocket_advance = read_all "tools/pocket_upgrade_advancement.json"
for _, pocket in ipairs { { "normal", "0" }, { "advanced", "1" } } do
    local pocket_family, pocket_data = pocket[1], pocket[2]
    for _, upgrade in ipairs(pocket_upgrades) do
        local upgrade_id, upgrade_item = upgrade[1], upgrade[2] or ('"%s"'):format(upgrade[1])
        local path = ("generated/pocket_%s/%s"):format(pocket_family, (upgrade_id:gsub(":", "_")))
        local keys = {
            upgrade_id = upgrade_id, upgrade_item = upgrade_item,
            pocket_family = pocket_family, pocket_data = pocket_data,
            path = path,
        }

        write_all("src/main/resources/assets/computercraft/recipes/" .. path .. ".json", template(pocket_recipe, keys))
        write_all("src/main/resources/assets/computercraft/advancements/recipes/" .. path .. ".json", template(pocket_advance, keys))
    end
end

-- Write disk recipe
local disk_recipe = read_all "tools/disk_recipe.json"
for i, colour in ipairs(colours) do
    local path = ("generated/disk/disk_%s"):format(i)
    local keys = {
            dye = i - 1, colour = colour,
            path = path,
        }

        write_all("src/main/resources/assets/computercraft/recipes/" .. path .. ".json", template(disk_recipe, keys))
end
