/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.item.ItemProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.SystemUtil;

import javax.annotation.Nonnull;

public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade
{
    private final Identifier id;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    public AbstractTurtleUpgrade( Identifier id, TurtleUpgradeType type, String adjective, ItemStack stack )
    {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    public AbstractTurtleUpgrade( Identifier id, TurtleUpgradeType type, String adjective, ItemProvider item )
    {
        this( id, type, adjective, new ItemStack( item ) );
    }

    public AbstractTurtleUpgrade( Identifier id, TurtleUpgradeType type, ItemStack stack )
    {
        this( id, type, SystemUtil.createTranslationKey( "upgrade", id ) + ".adjective", stack );
    }

    public AbstractTurtleUpgrade( Identifier id, TurtleUpgradeType type, ItemProvider item )
    {
        this( id, type, new ItemStack( item ) );
    }

    @Nonnull
    @Override
    public final Identifier getUpgradeId()
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
