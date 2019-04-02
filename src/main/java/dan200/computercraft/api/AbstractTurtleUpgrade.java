/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.api;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * A base class for {@link ITurtleUpgrade}s.
 *
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade
{
    private final ResourceLocation id;
    private final int legacyId;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    protected AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, String adjective, ItemStack stack )
    {
        this.id = id;
        this.legacyId = legacyId;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, String adjective, Item item )
    {
        this( id, legacyId, type, adjective, new ItemStack( item ) );
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, String adjective, Block block )
    {
        this( id, legacyId, type, adjective, new ItemStack( block ) );
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, ItemStack stack )
    {
        this( id, legacyId, type, "upgrade." + id + ".adjective", stack );
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, Item item )
    {
        this( id, legacyId, type, new ItemStack( item ) );
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, int legacyId, TurtleUpgradeType type, Block block )
    {
        this( id, legacyId, type, new ItemStack( block ) );
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
