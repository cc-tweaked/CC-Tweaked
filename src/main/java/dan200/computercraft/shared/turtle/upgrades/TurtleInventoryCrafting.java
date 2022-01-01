/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurtleInventoryCrafting extends CraftingContainer
{
    private final ITurtleAccess turtle;
    private int xStart = 0;
    private int yStart = 0;

    @SuppressWarnings( "ConstantConditions" )
    public TurtleInventoryCrafting( ITurtleAccess turtle )
    {
        // Passing null in here is evil, but we don't have a container present. We override most methods in order to
        // avoid throwing any NPEs.
        super( null, 0, 0 );
        this.turtle = turtle;
    }

    @Nullable
    private Recipe<CraftingContainer> tryCrafting( int xStart, int yStart )
    {
        this.xStart = xStart;
        this.yStart = yStart;

        // Check the non-relevant parts of the inventory are empty
        for( int x = 0; x < TileTurtle.INVENTORY_WIDTH; x++ )
        {
            for( int y = 0; y < TileTurtle.INVENTORY_HEIGHT; y++ )
            {
                if( x < this.xStart || x >= this.xStart + 3 ||
                    y < this.yStart || y >= this.yStart + 3 )
                {
                    if( !turtle.getInventory().getItem( x + y * TileTurtle.INVENTORY_WIDTH ).isEmpty() )
                    {
                        return null;
                    }
                }
            }
        }

        // Check the actual crafting
        return turtle.getLevel().getRecipeManager().getRecipeFor( RecipeType.CRAFTING, this, turtle.getLevel() ).orElse( null );
    }

    @Nullable
    public List<ItemStack> doCrafting( Level world, int maxCount )
    {
        if( world.isClientSide || !(world instanceof ServerLevel) ) return null;

        // Find out what we can craft
        Recipe<CraftingContainer> recipe = tryCrafting( 0, 0 );
        if( recipe == null ) recipe = tryCrafting( 0, 1 );
        if( recipe == null ) recipe = tryCrafting( 1, 0 );
        if( recipe == null ) recipe = tryCrafting( 1, 1 );
        if( recipe == null ) return null;

        // Special case: craft(0) just returns an empty list if crafting was possible
        if( maxCount == 0 ) return Collections.emptyList();

        TurtlePlayer player = TurtlePlayer.get( turtle );

        ArrayList<ItemStack> results = new ArrayList<>();
        for( int i = 0; i < maxCount && recipe.matches( this, world ); i++ )
        {
            ItemStack result = recipe.assemble( this );
            if( result.isEmpty() ) break;
            results.add( result );

            result.onCraftedBy( world, player, result.getCount() );
            ForgeEventFactory.firePlayerCraftingEvent( player, result, this );

            ForgeHooks.setCraftingPlayer( player );
            NonNullList<ItemStack> remainders = recipe.getRemainingItems( this );
            ForgeHooks.setCraftingPlayer( null );

            for( int slot = 0; slot < remainders.size(); slot++ )
            {
                ItemStack existing = getItem( slot );
                ItemStack remainder = remainders.get( slot );

                if( !existing.isEmpty() )
                {
                    removeItem( slot, 1 );
                    existing = getItem( slot );
                }

                if( remainder.isEmpty() ) continue;

                // Either update the current stack or add it to the remainder list (to be inserted into the inventory
                // afterwards).
                if( existing.isEmpty() )
                {
                    setItem( slot, remainder );
                }
                else if( ItemStack.isSame( existing, remainder ) && ItemStack.tagMatches( existing, remainder ) )
                {
                    remainder.grow( existing.getCount() );
                    setItem( slot, remainder );
                }
                else
                {
                    results.add( remainder );
                }
            }
        }

        return results;
    }

    @Override
    public int getWidth()
    {
        return 3;
    }

    @Override
    public int getHeight()
    {
        return 3;
    }

    private int modifyIndex( int index )
    {
        int x = xStart + index % getWidth();
        int y = yStart + index / getHeight();
        return x >= 0 && x < TileTurtle.INVENTORY_WIDTH && y >= 0 && y < TileTurtle.INVENTORY_HEIGHT
            ? x + y * TileTurtle.INVENTORY_WIDTH
            : -1;
    }

    // IInventory implementation

    @Override
    public int getContainerSize()
    {
        return getWidth() * getHeight();
    }

    @Nonnull
    @Override
    public ItemStack getItem( int i )
    {
        i = modifyIndex( i );
        return turtle.getInventory().getItem( i );
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int i )
    {
        i = modifyIndex( i );
        return turtle.getInventory().removeItemNoUpdate( i );
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int i, int size )
    {
        i = modifyIndex( i );
        return turtle.getInventory().removeItem( i, size );
    }

    @Override
    public void setItem( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        turtle.getInventory().setItem( i, stack );
    }

    @Override
    public int getMaxStackSize()
    {
        return turtle.getInventory().getMaxStackSize();
    }

    @Override
    public void setChanged()
    {
        turtle.getInventory().setChanged();
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
    {
        return true;
    }

    @Override
    public boolean canPlaceItem( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        return turtle.getInventory().canPlaceItem( i, stack );
    }

    @Override
    public void clearContent()
    {
        for( int i = 0; i < getContainerSize(); i++ )
        {
            int j = modifyIndex( i );
            turtle.getInventory().setItem( j, ItemStack.EMPTY );
        }
    }
}
