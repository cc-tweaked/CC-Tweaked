/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Extension to {@link ConventionalItemTags}.
 * <p>
 * Try to keep these consistent with <a href="https://fabricmc.net/wiki/tutorial:tags">the wiki page</a>.
 */
public class MoreConventionalTags {
    public static final TagKey<Item> SKULLS = item("skulls");
    public static final TagKey<Item> WOODEN_CHESTS = item("wooden_chests");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", name));
    }
}
