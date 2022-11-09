/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ComputerCraftTags.Blocks;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static dan200.computercraft.api.ComputerCraftTags.Items.*;

class ItemTagsGenerator extends ItemTagsProvider {
    ItemTagsGenerator(DataGenerator generator, BlockTagsGenerator blockTags, ExistingFileHelper helper) {
        super(generator, blockTags, ComputerCraftAPI.MOD_ID, helper);
    }

    @Override
    protected void addTags() {
        copy(Blocks.COMPUTER, COMPUTER);
        copy(Blocks.TURTLE, TURTLE);
        tag(WIRED_MODEM).add(ModRegistry.Items.WIRED_MODEM.get(), ModRegistry.Items.WIRED_MODEM_FULL.get());
        copy(Blocks.MONITOR, MONITOR);

        tag(ItemTags.PIGLIN_LOVED).add(
            ModRegistry.Items.COMPUTER_ADVANCED.get(), ModRegistry.Items.TURTLE_ADVANCED.get(),
            ModRegistry.Items.WIRELESS_MODEM_ADVANCED.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(),
            ModRegistry.Items.MONITOR_ADVANCED.get()
        );
    }
}
