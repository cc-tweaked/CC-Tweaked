/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.shared.platform.MoreConventionalTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.world.item.Items;

public class MoreConventionalTagsProvider extends FabricTagProvider.ItemTagProvider {
    public MoreConventionalTagsProvider(FabricDataGenerator dataGenerator) {
        super(dataGenerator);
    }

    @Override
    protected void generateTags() {
        tag(MoreConventionalTags.SKULLS).add(
            Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PLAYER_HEAD, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL,
            Items.ZOMBIE_HEAD
        );
        tag(MoreConventionalTags.WOODEN_CHESTS).add(Items.CHEST, Items.TRAPPED_CHEST);
    }
}
