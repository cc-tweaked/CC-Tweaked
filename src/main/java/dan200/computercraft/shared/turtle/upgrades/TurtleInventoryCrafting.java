/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class TurtleInventoryCrafting extends InventoryCrafting
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
                    if( !m_turtle.getInventory().getStackInSlot( x + y * TileTurtle.INVENTORY_WIDTH ).isEmpty() )
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Check the actual crafting
        return CraftingManager.findMatchingResult( this, m_turtle.getWorld() );
    }

    public ArrayList<ItemStack> doCrafting( World world, int maxCount )
    {
        if( world.isRemote || !(world instanceof WorldServer) )
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
            int size = getSizeInventory();
            if( maxCount > 1 )
            {
                int minStackSize = 0;
                for( int n = 0; n < size; n++ )
                {
                    ItemStack stack = getStackInSlot( n );
                    if( !stack.isEmpty() && (minStackSize == 0 || minStackSize > stack.getCount()) )
                    {
                        minStackSize = stack.getCount();
                    }
                }

                if( minStackSize > 1 )
                {
                    numToCraft = Math.min( minStackSize, result.getMaxStackSize() / result.getCount() );
                    numToCraft = Math.min( numToCraft, maxCount );
                    result.setCount( result.getCount() * numToCraft );
                }
            }

            // Do post-pickup stuff
            TurtlePlayer turtlePlayer = TurtlePlayer.get( m_turtle );
            result.onCrafting( world, turtlePlayer, numToCraft );
            results.add( result );

            // Consume resources from the inventory
            NonNullList<ItemStack> remainingItems = CraftingManager.getRemainingItems( this, world );
            for( int n = 0; n < size; n++ )
            {
                ItemStack stack = getStackInSlot( n );
                if( !stack.isEmpty() )
                {
                    decrStackSize( n, numToCraft );

                    ItemStack replacement = remainingItems.get( n );
                    if( !replacement.isEmpty() )
                    {
                        if( !(replacement.isItemStackDamageable() && replacement.getItemDamage() >= replacement.getMaxDamage()) )
                        {
                            replacement.setCount( Math.min( numToCraft, replacement.getMaxStackSize() ) );
                            if( getStackInSlot( n ).isEmpty() )
                            {
                                setInventorySlotContents( n, replacement );
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

    @Nonnull
    @Override
    public ItemStack getStackInRowAndColumn( int x, int y )
    {
        if( x >= 0 && x < getWidth() && y >= 0 && y < getHeight() )
        {
            return getStackInSlot( x + y * getWidth() );
        }
        return ItemStack.EMPTY;
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
    public int getSizeInventory()
    {
        return getWidth() * getHeight();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot( int i )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().getStackInSlot( i );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "";
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        return new TextComponentString( "" );
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot( int i )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().removeStackFromSlot( i );
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize( int i, int size )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().decrStackSize( i, size );
    }

    @Override
    public void setInventorySlotContents( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        m_turtle.getInventory().setInventorySlotContents( i, stack );
    }

    @Override
    public int getInventoryStackLimit()
    {
        return m_turtle.getInventory().getInventoryStackLimit();
    }

    @Override
    public void markDirty()
    {
        m_turtle.getInventory().markDirty();
    }

    @Override
    public boolean isUsableByPlayer( EntityPlayer player )
    {
        return true;
    }

    @Override
    public void openInventory( EntityPlayer player )
    {
    }

    @Override
    public void closeInventory( EntityPlayer player )
    {
    }

    @Override
    public boolean isItemValidForSlot( int i, @Nonnull ItemStack stack )
    {
        i = modifyIndex( i );
        return m_turtle.getInventory().isItemValidForSlot( i, stack );
    }

    @Override
    public int getField( int id )
    {
        return 0;
    }

    @Override
    public void setField( int id, int value )
    {
    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public void clear()
    {
        for( int i = 0; i < getSizeInventory(); i++ )
        {
            int j = modifyIndex( i );
            m_turtle.getInventory().setInventorySlotContents( j, ItemStack.EMPTY );
        }
    }
}
