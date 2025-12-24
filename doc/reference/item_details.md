---
module: [kind=reference] item_details
since: 1.64
changed: 1.94.0 Add NBT hash, item tags, lore, enchantment and unbreakable flag.
changed: 1.100.9 Add item groups.
changed: 1.117.0 Added map colour and potion effects.
---

<!--
SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Item details
Several functions in CC: Tweaked, such as [`turtle.getItemDetail`] and [`inventory.getItemDetail`] provide a way to get
information about an item stack. This page details information about items that CC: Tweaked may return.

Some methods (such as [`inventory.list`] and [`turtle.getItemDetail`] without the `detailed` flag), will only return
the "Basic information" about the item.

## Basic information
Item information will *always* contain:
 - `name: string`: The namespaced ID for this item, e.g. `minecraft:dirt`. See [the Minecraft wiki][item ids] for a
   list of vanilla item IDs.
 - `count: number`: The number of items in the stack.
 - `nbt?: string`: A hash of the NBT in the stack. While this does not expose any information about the item's NBT, it
   can be used as a way to compare items. If two items have the same `name` and `nbt`, then all other properties
   (e.g. durability, enchantment) will be the same.

### Example
A stack of 32 Stripped Acacia Logs:

```lua {data-no-run=1}
{
    name = "minecraft:stripped_acacia_log",
    count = 32,
}
```

A turtle with an upgrade attached:

```lua {data-no-run=1}
{
    name = "computercraft:turtle_normal",
    count = 1,
    nbt = "a33095c2eb17c10e12f2b970c4e1b2bb",
}
```

## Display information
Common information shown in the item's tooltip:

 - `displayName: string`: The translated display name of the item. This uses the *server's* language. This will
   typically be English on multi-player servers, and your current language on single player.
 - `lore: { string... }`: Additional lore about this item, as a list of strings.

### Example
A stack of Stripped Acacia Logs:

```lua {data-no-run=1}
{
    name = "minecraft:stripped_acacia_log",
    count = 32,
    displayName = "Stripped Acacia Log",
}
```

## Max count
The maximum number of items this item can stack to:

 - `maxCount: number`: The max possible size of the item stack.

### Example
A stack of Stripped Acacia Logs:

```lua {data-no-run=1}
{
    name = "minecraft:stripped_acacia_log",
    count = 32,
    maxCount = 64,
}
```

## Item tags
The [tags][item tags] an item has.

 - `tags: { [string] = boolean }`: The set of tags for this item. This is a mapping of tag name to `true`.

While the representation of tags is a little more complicated then a single list, this makes it very easy to check if an
item has a certain tag:

```lua
--- Check if the item in the turtle's inventory is a log.
local function is_log(slot)
    local ok, block = turtle.getItemDetail(slot, true)
    return ok and block.tags["minecraft:logs"]
end
```

### Example
A stack of Stripped Acacia Logs:

```lua {data-no-run=1}
{
    name = "minecraft:stripped_acacia_log",
    count = 32,
    tags = {
        ["minecraft:acacia_logs"] = true,
        ["minecraft:logs"] = true,
        ["minecraft:logs_that_burn"] = true,
    }
}
```

## Item groups
The creative tabs this item appears on:

 - `itemGroups: { { id = string, displayName = string }... }`: The item groups this item appears on. Each item group is
   stored as a table, containing its id and display name.

> [Version differences][!INFO]
> This information is not available on Minecraft 1.19.3 to 1.20.3. This field is present, but empty on those versions.

### Example
A stack of Stripped Acacia Logs:

```lua {data-no-run=1}
{
    name = "minecraft:stripped_acacia_log",
    count = 32,
    itemGroups = {
        {
            id = "minecraft:building_blocks",
            displayName = "Building Blocks"
        }
    }
}
```

## Damage and durability
If this item can be damaged (e.g. a pickaxe), then its damage and durability will be available:
 - `damage: number`: The amount of damage this item has taken.
 - `maxDamage: number`: The maximum amount of damage this item has taken.
 - `durability?: number`: If this item is damaged (i.e. the durability bar is visible), the percentage left on the
   durability bar, between 0 and 1 (inclusive).
 - `unbreakable?: boolean`: `true`, if the item is nubreakable

### Example
An unused diamond pickaxe:

```lua {data-no-run=1}
{
    name = "minecraft:diamond_pickaxe",
    count = 1,
    damage = 0,
    maxDamage = 1561,
}
```

A half-used wooden pickaxe:

```lua {data-no-run=1}
{
    name = "minecraft:wooden_pickaxe",
    count = 1,
    damage = 21,
    maxDamage = 59,
    durability = 0.615,
}
```

## Enchantments
The enchantments this item has. This includes both tools and enchanted books.

 - `enchantments: { table... }`: The enchantments this item has. Each enchantment is a table containing several
   properties:
    - `name: string`: The namespaced ID for this enchantment, e.g. `minecraft:efficiency`. See [the Minecraft
      wiki][enchantment ids] for a list of vanilla enchantment IDs.
    - `displayName: string`: The translated display name for this enchantment.
    - `level: number`: The level for this enchantment.

### Example
A diamond pickaxe with Efficiency V:
```lua {data-no-run=1}
{
    name = "minecraft:diamond_pickaxe",
    count = 1,
    enchantments = {
        {
            name = "minecraft:efficiency",
            level = 5,
            displayName = "Efficiency V",
        }
    }
}
```

## Potion effects
The effects this potion (or potion-embued item, such as a tipped arrow) has:

 - `potionEffects: { table... }`: The effects this item has. Each potion effect is a table containing several
   properties:
    - `name: string`: The namespaced ID for this effect, e.g. `minecraft:regeneration`. See [the Minecraft wiki][effect
      ids] for a list of vanilla effect IDs.
    - `displayName: string`: The translated display name for this potion effect.
    - `duration?: number`: The duration this effect will last for (if not instant), in seconds.
    - `potency?: number`: The potency of this potion.

### Example
A basic Potion of Healing:

```lua {data-no-run=1}
{
    name = "minecraft:potion",
    displayName = "Potion of Healing",
    potionEffects = {
        {
            name = "minecraft:instant_health",
            displayName = "Instant Health",
        },
    },
}
```

An upgraded Potion of Regeneration:

```lua {data-no-run=1}
{
    name = "minecraft:potion",
    displayName = "Potion of Regeneration",
    potionEffects = {
        {
            name = "minecraft:regeneration",
            displayName = "Regeneration II",
            duration = 22.5,
            potency = 2,
        },
    },
}
```

## Map colour
The colour the item's block form will appear on the map, if specified.

 - `mapColour?: number`: The colour of the block, as an RGB hex value.
 - `mapColor?: number`: The color of the block, as an RGB hex value.

The map colour is just returned as a plain number (e.g. `9923917` for dirt). It can either be displayed in hex with
[`string.format`], or converted to individual RGB values with [`colors.unpackRGB`].

### Example
A stack of Stripped Acacia Logs:

```lua {data-no-run=1}
{
    name = "minecraft:stripped_acacia_log",
    count = 32,
    mapColour = 14188339,
    mapColor = 14188339,
}
```

[item ids]: https://minecraft.wiki/w/Java_Edition_data_values#Items "Java Edition data values on the Minecraft Wiki"
[item tags]:https://minecraft.wiki/w/Item_tag_%28Java_Edition%29 "Item tags on the Minecraft Wiki"
[effect ids]: https://minecraft.wiki/w/Java_Edition_data_values#Effects "Java Edition data values on the Minecraft Wiki"
[enchantment ids]: https://minecraft.wiki/w/Java_Edition_data_values#Enchantments "Java Edition data values on the Minecraft Wiki"
