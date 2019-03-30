/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
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
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurtleInventoryCrafting extends InventoryCrafting
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
    private IRecipe tryCrafting( int xStart, int yStart )
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
                        return null;
                    }
                }
            }
        }

        // Check the actual crafting
        return CraftingManager.findMatchingRecipe( this, m_turtle.getWorld() );
    }

    @Nullable
    public List<ItemStack> doCrafting( World world, int maxCount )
    {
        if( world.isRemote || !(world instanceof WorldServer) ) return null;

        // Find out what we can craft
        IRecipe recipe = tryCrafting( 0, 0 );
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
            ItemStack result = recipe.getCraftingResult( this );
            if( result.isEmpty() ) break;
            results.add( result );

            result.onCrafting( world, player, result.getCount() );
            FMLCommonHandler.instance().firePlayerCraftingEvent( player, result, this );

            ForgeHooks.setCraftingPlayer( player );
            NonNullList<ItemStack> remainders = recipe.getRemainingItems( this );
            ForgeHooks.setCraftingPlayer( null );

            for( int slot = 0; slot < remainders.size(); slot++ )
            {
                ItemStack existing = getStackInSlot( slot );
                ItemStack remainder = remainders.get( slot );

                if( !existing.isEmpty() )
                {
                    decrStackSize( slot, 1 );
                    existing = getStackInSlot( slot );
                }

                if( remainder.isEmpty() ) continue;

                // Either update the current stack or add it to the remainder list (to be inserted into the inventory
                // afterwards).
                if( existing.isEmpty() )
                {
                    setInventorySlotContents( slot, remainder );
                }
                else if( ItemStack.areItemsEqual( existing, remainder ) && ItemStack.areItemStackTagsEqual( existing, remainder ) )
                {
                    remainder.grow( existing.getCount() );
                    setInventorySlotContents( slot, remainder );
                }
                else
                {
                    results.add( remainder );
                }
            }
        }

        return results;
    }

    @Nonnull
    @Override
    public ItemStack getStackInRowAndColumn( int x, int y )
    {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight()
            ? getStackInSlot( x + y * getWidth() )
            : ItemStack.EMPTY;
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
