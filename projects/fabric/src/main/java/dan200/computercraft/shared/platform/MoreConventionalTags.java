// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
