/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeTabMain extends CreativeModeTab {
    public CreativeTabMain() {
        super(ComputerCraftAPI.MOD_ID);
    }

    @Override
    public ItemStack makeIcon() {
        return new ItemStack(ModRegistry.Blocks.COMPUTER_NORMAL.get());
    }
}
