/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import javax.annotation.Nonnull;

public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade
{
    private final ResourceLocation id;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    public AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, String adjective, ItemStack stack )
    {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    public AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, String adjective, IItemProvider item )
    {
        this( id, type, adjective, new ItemStack( item ) );
    }

    public AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, ItemStack stack )
    {
        this( id, type, Util.makeTranslationKey( "upgrade", id ) + ".adjective", stack );
    }

    public AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, IItemProvider item )
    {
        this( id, type, new ItemStack( item ) );
    }

    @Nonnull
    @Override
    public final ResourceLocation getUpgradeId()
    {
        return id;
    }

    @Nonnull
    @Override
    public final String getUnlocalisedAdjective()
    {
        return adjective;
    }

    @Nonnull
    @Override
    public final TurtleUpgradeType getType()
    {
        return type;
    }

    @Nonnull
    @Override
    public final ItemStack getCraftingItem()
    {
        return stack;
    }
}
