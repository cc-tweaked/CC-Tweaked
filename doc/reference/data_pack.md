---
module: [kind=reference] data_pack
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Data packs
CC: Tweaked allows customising and extending many of its features via [data packs][data pack wiki], such as adding new
programs to the ROM, or providing new turtle tools. This page serves as a reference for CC:T's data pack functionality.

> [Data pack compatibility][!NOTE]
>
> Unlike other parts of this documentation, the format of data packs change between Minecraft versions. Make sure you're
> using the right documentation for your Minecraft version.
>
>  - [Minecraft 1.20.1](/mc-1.20.x/reference/data_pack.html)
>  - [Minecraft 1.21.1](/mc-1.21.x/reference/data_pack.html)
>  - [Minecraft 26.3](/mc-26.3/reference/data_pack.html)

In addition to this documentation, we also provide an [online data pack builder][data pack builder] and an
[example/template data pack][data pack example].

## CraftOS ROM
Datapacks can be used to add or overwrite files in CraftOS's ROM, allowing you to add new programs or modules, or adjust
the behaviour of existing ones. Files should be placed in `data/computercraft/lua/rom`.  For example, a datapack
containing `data/computercraft/lua/rom/programs/example.lua` will add a new program `example`.

> [File naming][!WARNING]
>
> Data packs [impose some restrictions on file names][legal data pack]. As a result, all file and directory names must
> be lower case.

 - Files under `rom/autorun` will be run on computer startup.
 - Files under `rom/modules/main` can be loaded via `require`.
 - Files located in `rom/apis/` will be loaded via `os.loadAPI` on startup. Similarly:
   - Files under `rom/apis/http` will be loaded if the HTTP API is enabled.
   - Files under `rom/apis/turtle` will be loaded on all turtles.
   - Files under `rom/apis/pocket` will be loaded on all pocket computers.
   - Files under `rom/apis/command` will be loaded on all command computers.

## Tags
CC:T provides several [block][block tags] and [item][item tags] tags. While most tags are just used to group related
blocks/items (e.g. `computercraft:turtles` is the list of all turtles), several can be extended by data packs to tweak
functionality:

 - `computercraft:turtle_can_place` item tag: This tag lists items turtles can use with [`turtle.place`]. By default
   this contains glass bottles and boats,
 - `computercraft:turtle_can_use` block tag: This tag lists blocks turtles can interact with [`turtle.place`]. By
   default this contains beehives, cauldrons and the composter. The [data pack builder] has options for adding
   levers/buttons and modems to this list, allowing turtles to toggle them.
 - `computercraft:always_breakable` block tag: All turtle tools are capable of breaking instant-break blocks, such as
   saplings. This tag specifies additional blocks that can be broken by all tools. By default, this includes leaves,
   bamboo and bamboo saplings.
 - `computercraft:turtle_shovel_harvestable`, `computercraft:turtle_sword_harvestable`,
   `computercraft:turtle_hoe_harvestable` block tags: These tags specify which blocks can be broken by the shovel, sword
   and hoe respectively.

[block tags]: ../../javadoc/dan200/computercraft/api/ComputerCraftTags.Blocks.html "ComputerCraftTags.Blocks javadoc"
[data pack builder]: https://datapacks.madefor.cc/
[data pack example]: https://github.com/cc-tweaked/datapack-example "An example data pack for CC: Tweaked"
[data pack wiki]: https://minecraft.wiki/w/Data_pack "Data pack on the Minecraft wiki"
[item tags]: ../../javadoc/dan200/computercraft/api/ComputerCraftTags.Items.html "ComputerCraftTags.Items javadoc"
[legal data pack]: https://minecraft.wiki/w/Tutorials/Creating_a_data_pack#Legal_characters
