---
module: [kind=reference] entity_details
since: 1.118.0
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Entity details
Some functions in CC: Tweaked (such as [`commands.getEntities`]) provide a way to get information about an entity.
This page details information about entities that CC: Tweaked may return.

## Basic information
Entity information will *always* contain:
 - `name: string`: The namespaced ID for this entity, e.g. `minecraft:player`. See [the Minecraft wiki][entity ids] for a
   list of vanilla entity IDs.

### Example
A creeper:

```lua {data-no-run=1}
{
    name = "minecraft:creeper",
    count = 32,
}
```

## Display information
The display name of the entity.

 - `displayName: string`: The translated display name of the entity. This uses the *server's* language. This will
   typically be English on multi-player servers, and your current language on single player.

### Example
A creeper:

```lua {data-no-run=1}
{
    name = "minecraft:creeper",
    displayName = "Creeper",
}
```

## Health
The health of an entity.

 - `health: number`: The current health of the entity.
 - `maxHealth: number`: The maximum health of an entity.

### Example
A player at max health.

```lua {data-no-run=1}
{
    name = "minecraft:player",
    displayName = "Alex",
    health = 20,
    maxHealth = 20,
}
```

## Entity tags
The [tags][entity tags] an entity of this type has.

 - `tags: { [string] = boolean }`: The set of tags for this entity. This is a mapping of tag name to `true`.

[entity ids]: https://minecraft.wiki/w/Java_Edition_data_values#Entities "Java Edition data values on the Minecraft Wiki"
[entity tags]: https://minecraft.wiki/w/Entity_type_tag_%28Java_Edition%29 "Entity type tags on the Minecraft Wiki"
