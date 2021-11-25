/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.pocket;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * A base class for {@link IPocketUpgrade}s.
 *
 * One does not have to use this, but it does provide a convenient template.
 */
public abstract class AbstractPocketUpgrade implements IPocketUpgrade
{
    private final ResourceLocation id;
    private final String adjective;
    private final NonNullSupplier<ItemStack> stack;

    protected AbstractPocketUpgrade( ResourceLocation id, String adjective, NonNullSupplier<ItemStack> stack )
    {
        this.id = id;
        this.adjective = adjective;
        this.stack = stack;
    }

    protected AbstractPocketUpgrade( ResourceLocation id, NonNullSupplier<ItemStack> item )
    {
        this( id, Util.makeDescriptionId( "upgrade", id ) + ".adjective", item );
    }

    protected AbstractPocketUpgrade( ResourceLocation id, String adjective, ItemStack stack )
    {
        this( id, adjective, () -> stack );
    }

    protected AbstractPocketUpgrade( ResourceLocation id, ItemStack stack )
    {
        this( id, () -> stack );
    }

    protected AbstractPocketUpgrade( ResourceLocation id, String adjective, IItemProvider item )
    {
        this( id, adjective, new CachedStack( () -> item ) );
    }

    protected AbstractPocketUpgrade( ResourceLocation id, IItemProvider item )
    {
        this( id, new CachedStack( () -> item ) );
    }

    protected AbstractPocketUpgrade( ResourceLocation id, String adjective, Supplier<? extends IItemProvider> item )
    {
        this( id, adjective, new CachedStack( item ) );
    }

    protected AbstractPocketUpgrade( ResourceLocation id, Supplier<? extends IItemProvider> item )
    {
        this( id, new CachedStack( item ) );
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
    public final ItemStack getCraftingItem()
    {
        return stack.get();
    }

    /**
     * Caches the construction of an item stack.
     *
     * @see dan200.computercraft.api.turtle.AbstractTurtleUpgrade For explanation of this class.
     */
    private static final class CachedStack implements NonNullSupplier<ItemStack>
    {
        private final Supplier<? extends IItemProvider> provider;
        private Item item;
        private ItemStack stack;

        CachedStack( Supplier<? extends IItemProvider> provider )
        {
            this.provider = provider;
        }

        @Nonnull
        @Override
        public ItemStack get()
        {
            Item item = provider.get().asItem();
            if( item == this.item && stack != null ) return stack;
            return stack = new ItemStack( this.item = item );
        }
    }
}
