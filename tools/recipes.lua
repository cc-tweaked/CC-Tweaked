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
    { "computercraft:wireless_modem_normal" },
    { "computercraft:wireless_modem_advanced" },
    { "computercraft:speaker" },
    { "minecraft:crafting_table" },
    { "minecraft:diamond_sword" },
    { "minecraft:diamond_shovel" },
    { "minecraft:diamond_pickaxe" },
    { "minecraft:diamond_axe" },
    { "minecraft:diamond_hoe" },
}

--- All pocket upgrades, and an optional item for this upgrade.
local pocket_upgrades = {
    { "computercraft:wireless_modem_normal" },
    { "computercraft:wireless_modem_advanced" },
    { "computercraft:speaker" },
}

--- All dye/disk colours
local colours = {
    { 0x111111, "minecraft:black_dye" },
    { 0xcc4c4c, "minecraft:red_dye" },
    { 0x57A64E, "minecraft:green_dye" },
    { 0x7f664c, "minecraft:brown_dye" },
    { 0x3366cc, "minecraft:blue_dye" },
    { 0xb266e5, "minecraft:purple_dye" },
    { 0x4c99b2, "minecraft:cyan_dye" },
    { 0x999999, "minecraft:light_gray_dye" },
    { 0x4c4c4c, "minecraft:gray_dye" },
    { 0xf2b2cc, "minecraft:pink_dye" },
    { 0x7fcc19, "minecraft:lime_dye" },
    { 0xdede6c, "minecraft:yellow_dye" },
    { 0x99b2f2, "minecraft:light_blue_dye" },
    { 0xe57fd8, "minecraft:magenta_dye" },
    { 0xf2b233, "minecraft:orange_dye" },
    { 0xf0f0f0, "minecraft:white_dye" },
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
    return str:gsub("%$%{([^}]+)%}", function(k)
        return subs[k] or error("Unknown key " .. k)
    end)
end

-- Write turtle upgrades
local turtle_recipe = read_all "tools/turtle_upgrade_recipe.json"
local turtle_advance = read_all "tools/turtle_upgrade_advancement.json"
for _, turtle_family in ipairs { "normal", "advanced" } do
    for _, upgrade in ipairs(turtle_upgrades) do
        local upgrade_id, upgrade_item = upgrade[1], upgrade[2] or upgrade[1]
        local path = ("generated/turtle_%s/%s"):format(turtle_family, (upgrade_id:gsub(":", "_")))
        local keys = {
            upgrade_id = upgrade_id, upgrade_item = upgrade_item,
            turtle_family = turtle_family,
            path = path,
        }

        write_all("src/main/resources/data/computercraft/recipes/" .. path .. ".json", template(turtle_recipe, keys))
        write_all("src/main/resources/data/computercraft/advancements/recipes/" .. path .. ".json", template(turtle_advance, keys))
    end
end

-- Write pocket upgrades
local pocket_recipe = read_all "tools/pocket_upgrade_recipe.json"
local pocket_advance = read_all "tools/pocket_upgrade_advancement.json"
for _, pocket_family in ipairs { "normal", "advanced" } do
    for _, upgrade in ipairs(pocket_upgrades) do
        local upgrade_id, upgrade_item = upgrade[1], upgrade[2] or upgrade[1]
        local path = ("generated/pocket_%s/%s"):format(pocket_family, (upgrade_id:gsub(":", "_")))
        local keys = {
            upgrade_id = upgrade_id, upgrade_item = upgrade_item,
            pocket_family = pocket_family,
            path = path,
        }

        write_all("src/main/resources/data/computercraft/recipes/" .. path .. ".json", template(pocket_recipe, keys))
        write_all("src/main/resources/data/computercraft/advancements/recipes/" .. path .. ".json", template(pocket_advance, keys))
    end
end

-- Write disk recipe
local disk_recipe = read_all "tools/disk_recipe.json"
for i, colour in ipairs(colours) do
    local path = ("generated/disk/disk_%s"):format(i)
    local keys = {
        dye = colour[2], colour = colour[1],
        path = path,
    }

    write_all("src/main/resources/data/computercraft/recipes/" .. path .. ".json", template(disk_recipe, keys))
end
