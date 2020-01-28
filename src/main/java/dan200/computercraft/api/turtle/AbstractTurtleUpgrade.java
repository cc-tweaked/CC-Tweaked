/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import javax.annotation.Nonnull;

/**
 * A base class for {@link ITurtleUpgrade}s.
 *
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractTurtleUpgrade implements ITurtleUpgrade
{
    private final ResourceLocation id;
    private final TurtleUpgradeType type;
    private final String adjective;
    private final ItemStack stack;

    protected AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, String adjective, ItemStack stack )
    {
        this.id = id;
        this.type = type;
        this.adjective = adjective;
        this.stack = stack;
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, String adjective, IItemProvider item )
    {
        this( id, type, adjective, new ItemStack( item ) );
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, ItemStack stack )
    {
        this( id, type, Util.makeTranslationKey( "upgrade", id ) + ".adjective", stack );
    }

    protected AbstractTurtleUpgrade( ResourceLocation id, TurtleUpgradeType type, IItemProvider item )
    {
        this( id, type, new ItemStack( item ) );
    }

    @Nonnull
    @Override
    public final ResourceLocation getUpgradeID()
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
