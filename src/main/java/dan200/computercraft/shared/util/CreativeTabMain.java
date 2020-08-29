/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import javax.annotation.Nonnull;

import dan200.computercraft.ComputerCraft;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class CreativeTabMain extends ItemGroup {
    public CreativeTabMain(int i) {
        super(i, ComputerCraft.MOD_ID);
    }

    @Nonnull
    @Override
    @Environment (EnvType.CLIENT)
    public ItemStack createIcon() {
        return new ItemStack(ComputerCraft.Blocks.computerNormal);
    }
}
