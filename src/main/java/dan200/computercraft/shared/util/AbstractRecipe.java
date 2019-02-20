/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public abstract class AbstractRecipe implements IRecipe
{
    private final ResourceLocation id;

    public AbstractRecipe( ResourceLocation id )
    {
        this.id = id;
    }

    @Nonnull
    @Override
    public ItemStack getRecipeOutput()
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ResourceLocation getId()
    {
        return id;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }
}
