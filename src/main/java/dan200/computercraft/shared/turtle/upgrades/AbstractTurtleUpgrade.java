/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade
{
    private final ResourceLocation id;
    private final int legacyId;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    public AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, String adjective, ItemStack stack )
    {
        this.id = id;
        this.legacyId = legacyId;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    public AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, String adjective, Item item )
    {
        this( id, legacyId, type, adjective, new ItemStack( item ) );
    }

    public AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, String adjective, Block block )
    {
        this( id, legacyId, type, adjective, new ItemStack( block ) );
    }

    @Nonnull
    @Override
    public final ResourceLocation getUpgradeID()
    {
        return id;
    }

    @Override
    public final int getLegacyUpgradeID()
    {
        return legacyId;
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
