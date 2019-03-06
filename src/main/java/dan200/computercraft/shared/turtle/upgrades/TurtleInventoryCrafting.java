/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DefaultedList;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class TurtleInventoryCrafting extends CraftingInventory
{
    private ITurtleAccess m_turtle;
    private int m_xStart;
    private int m_yStart;

    public TurtleInventoryCrafting( ITurtleAccess turtle )
    {
        super( null, 0, 0 );
        m_turtle = turtle;
        m_xStart = 0;
        m_yStart = 0;
    }

    @Nonnull
    private ItemStack tryCrafting( int xStart, int yStart )
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
                    if( !m_turtle.getInventory().getInvStack( x + y * TileTurtle.INVENTORY_WIDTH ).isEmpty() )
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Check the actual crafting
        RecipeManager manager = m_turtle.getWorld().getServer().getRecipeManager();
        // TODO: This!!!!
        return manager.get( RecipeType.CRAFTING, this, m_turtle.getWorld() ).map( x -> x.getOutput() ).orElse( ItemStack.EMPTY );
    }

    public ArrayList<ItemStack> doCrafting( World world, int maxCount )
    {
        if( world.isClient || !(world instanceof ServerWorld) )
        {
            return null;
        }

        // Find out what we can craft
        ItemStack result = tryCrafting( 0, 0 );
        if( result.isEmpty() )
        {
            result = tryCrafting( 0, 1 );
        }
        if( result.isEmpty() )
        {
            result = tryCrafting( 1, 0 );
        }
        if( result.isEmpty() )
        {
            result = tryCrafting( 1, 1 );
        }

        // Craft it
        if( !result.isEmpty() )
        {
            // Special case: craft(0) just returns an empty list if crafting was possible
            ArrayList<ItemStack> results = new ArrayList<>();
            if( maxCount == 0 )
            {
                return results;
            }

            // Find out how many we can craft
            int numToCraft = 1;
            int size = getInvSize();
            if( maxCount > 1 )
            {
                int minStackSize = 0;
                for( int n = 0; n < size; n++ )
                {
                    ItemStack stack = getInvStack( n );
                    if( !stack.isEmpty() && (minStackSize == 0 || minStackSize > stack.getAmount()) )
                    {
                        minStackSize = stack.getAmount();
                    }
                }

                if( minStackSize > 1 )
                {
                    numToCraft = Math.min( minStackSize, result.getMaxAmount() / result.getAmount() );
                    numToCraft = Math.min( numToCraft, maxCount );
                    result.setAmount( result.getAmount() * numToCraft );
                }
            }

            // Do post-pickup stuff
            TurtlePlayer turtlePlayer = TurtlePlayer.get( m_turtle );
            result.onCrafted( world, turtlePlayer, numToCraft );
            results.add( result );

            // Consume resources from the inventory
            DefaultedList<ItemStack> remainingItems = world.getRecipeManager().method_8128( RecipeType.CRAFTING, this, world );
            for( int n = 0; n < size; n++ )
            {
                ItemStack stack = getInvStack( n );
                if( !stack.isEmpty() )
                {
                    takeInvStack( n, numToCraft );

                    ItemStack replacement = remainingItems.get( n );
                    if( !replacement.isEmpty() )
                    {
                        if( !(replacement.getItem().canDamage() && replacement.getDamage() >= replacement.getDurability()) )
                        {
                            replacement.setAmount( Math.min( numToCraft, replacement.getMaxAmount() ) );
                            if( getInvStack( n ).isEmpty() )
                            {
                                setInvStack( n, replacement );
                            }
                            else
                            {
                                results.add( replacement );
                            }
                        }
                    }
                }
            }
            return results;
        }

        return null;
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
        int x = m_xStart + (index % getWidth());
        int y = m_yStart + (index / getHeight());
        if( x >= 0 && x < TileTurtle.INVENTORY_WIDTH &&
            y >= 0 && y < TileTurtle.INVENTORY_HEIGHT )
        {
            return x + y * TileTurtle.INVENTORY_WIDTH;
        }
        return -1;
    }

    // IInventory implementation

    @Override
    public int getInvSize()
    {
        return getWidth() * getHeight();
    }

    @Nonnull
    @Override
    public ItemStack getInvStack( int i )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().getInvStack( i );
    }

    @Nonnull
    @Override
    public ItemStack removeInvStack( int i )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().removeInvStack( i );
    }

    @Nonnull
    @Override
    public ItemStack takeInvStack( int i, int size )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().takeInvStack( i, size );
    }

    @Override
    public void setInvStack( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        m_turtle.getInventory().setInvStack( i, stack );
    }

    @Override
    public int getInvMaxStackAmount()
    {
        return m_turtle.getInventory().getInvMaxStackAmount();
    }

    @Override
    public void markDirty()
    {
        m_turtle.getInventory().markDirty();
    }

    @Override
    public boolean canPlayerUseInv( PlayerEntity player )
    {
        return true;
    }

    @Override
    public boolean isValidInvStack( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().isValidInvStack( i, stack );
    }

    @Override
    public void clear()
    {
        for( int i = 0; i < getInvSize(); i++ )
        {
            int j = modifyIndex( i );
            m_turtle.getInventory().setInvStack( j, ItemStack.EMPTY );
        }
    }
}
