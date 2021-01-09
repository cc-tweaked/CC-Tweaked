/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.hooks.BasicEventHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurtleInventoryCrafting extends CraftingInventory
{
    private ITurtleAccess m_turtle;
    private int m_xStart;
    private int m_yStart;

    @SuppressWarnings( "ConstantConditions" )
    public TurtleInventoryCrafting( ITurtleAccess turtle )
    {
        // Passing null in here is evil, but we don't have a container present. We override most methods in order to
        // avoid throwing any NPEs.
        super( null, 0, 0 );
        m_turtle = turtle;
        m_xStart = 0;
        m_yStart = 0;
    }

    @Nullable
    private IRecipe<CraftingInventory> tryCrafting( int xStart, int yStart )
    {
        m_xStart = xStart;
        m_yStart = yStart;

        // Check the non-relevant parts of the inventory are empty
        for( int x = 0; x < TileTurtle.INVENTORY_WIDTH; x++ )
        {
            for( int y = 0; y < TileTurtle.INVENTORY_HEIGHT; y++ )
            {
                if( x < m_xStart || x >= m_xStart + 3 ||
                    y < m_yStart || y >= m_yStart + 3 )
                {
                    if( !m_turtle.getInventory().getItem( x + y * TileTurtle.INVENTORY_WIDTH ).isEmpty() )
                    {
                        return null;
                    }
                }
            }
        }

        // Check the actual crafting
        return m_turtle.getWorld().getRecipeManager().getRecipeFor( IRecipeType.CRAFTING, this, m_turtle.getWorld() ).orElse( null );
    }

    @Nullable
    public List<ItemStack> doCrafting( World world, int maxCount )
    {
        if( world.isClientSide || !(world instanceof ServerWorld) ) return null;

        // Find out what we can craft
        IRecipe<CraftingInventory> recipe = tryCrafting( 0, 0 );
        if( recipe == null ) recipe = tryCrafting( 0, 1 );
        if( recipe == null ) recipe = tryCrafting( 1, 0 );
        if( recipe == null ) recipe = tryCrafting( 1, 1 );
        if( recipe == null ) return null;

        // Special case: craft(0) just returns an empty list if crafting was possible
        if( maxCount == 0 ) return Collections.emptyList();

        TurtlePlayer player = TurtlePlayer.get( m_turtle );

        ArrayList<ItemStack> results = new ArrayList<>();
        for( int i = 0; i < maxCount && recipe.matches( this, world ); i++ )
        {
            ItemStack result = recipe.assemble( this );
            if( result.isEmpty() ) break;
            results.add( result );

            result.onCraftedBy( world, player, result.getCount() );
            BasicEventHooks.firePlayerCraftingEvent( player, result, this );

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
        int x = m_xStart + index % getWidth();
        int y = m_yStart + index / getHeight();
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
        return m_turtle.getInventory().getItem( i );
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int i )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().removeItemNoUpdate( i );
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int i, int size )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().removeItem( i, size );
    }

    @Override
    public void setItem( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        m_turtle.getInventory().setItem( i, stack );
    }

    @Override
    public int getMaxStackSize()
    {
        return m_turtle.getInventory().getMaxStackSize();
    }

    @Override
    public void setChanged()
    {
        m_turtle.getInventory().setChanged();
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
    {
        return true;
    }

    @Override
    public boolean canPlaceItem( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().canPlaceItem( i, stack );
    }

    @Override
    public void clearContent()
    {
        for( int i = 0; i < getContainerSize(); i++ )
        {
            int j = modifyIndex( i );
            m_turtle.getInventory().setItem( j, ItemStack.EMPTY );
        }
    }
}
