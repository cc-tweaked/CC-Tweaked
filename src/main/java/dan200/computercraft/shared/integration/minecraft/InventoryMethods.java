/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.minecraft;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.asm.GenericSource;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.versions.forge.ForgeVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static dan200.computercraft.shared.integration.minecraft.ArgumentHelpers.assertBetween;

@AutoService( GenericSource.class )
public class InventoryMethods implements GenericSource
{
    @Nonnull
    @Override
    public ResourceLocation id()
    {
        return new ResourceLocation( ForgeVersion.MOD_ID, "inventory" );
    }

    @Nonnull
    public static LazyOptional<IItemHandler> get( @Nullable TileEntity tile )
    {
        return tile == null ? LazyOptional.empty() : tile.getCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY );
    }

    @LuaFunction( mainThread = true )
    public static int size( IItemHandler inventory )
    {
        return inventory.getSlots();
    }

    @LuaFunction( mainThread = true )
    public static Map<Integer, Map<String, ?>> list( IItemHandler inventory )
    {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        int size = inventory.getSlots();
        for( int i = 0; i < size; i++ )
        {
            ItemStack stack = inventory.getStackInSlot( i );
            if( !stack.isEmpty() ) result.put( i + 1, fillBasicMeta( new HashMap<>( 4 ), stack ) );
        }

        return result;
    }

    @LuaFunction( mainThread = true )
    public static int pushItems(
        IItemHandler from, IComputerAccess computer,
        String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException
    {
        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( toName );
        if( location == null ) throw new LuaException( "Target '" + toName + "' does not exist" );

        IItemHandler to = extractHandler( location.getTarget() );
        if( to == null ) throw new LuaException( "Target '" + toName + "' is not an inventory" );

        // Validate slots
        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        if( actualLimit <= 0 ) throw new LuaException( "Limit must be > 0" );
        assertBetween( fromSlot, 1, from.getSlots(), "From slot out of range (%s)" );
        if( toSlot.isPresent() ) assertBetween( toSlot.get(), 1, to.getSlots(), "To slot out of range (%s)" );

        return moveItem( from, fromSlot - 1, to, toSlot.orElse( 0 ) - 1, actualLimit );
    }

    @LuaFunction( mainThread = true )
    public static int pullItems(
        IItemHandler to, IComputerAccess computer,
        String fromName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot
    ) throws LuaException
    {
        // Find location to transfer to
        IPeripheral location = computer.getAvailablePeripheral( fromName );
        if( location == null ) throw new LuaException( "Source '" + fromName + "' does not exist" );

        IItemHandler from = extractHandler( location.getTarget() );
        if( from == null ) throw new LuaException( "Source '" + fromName + "' is not an inventory" );

        // Validate slots
        int actualLimit = limit.orElse( Integer.MAX_VALUE );
        if( actualLimit <= 0 ) throw new LuaException( "Limit must be > 0" );
        assertBetween( fromSlot, 1, from.getSlots(), "From slot out of range (%s)" );
        if( toSlot.isPresent() ) assertBetween( toSlot.get(), 1, to.getSlots(), "To slot out of range (%s)" );

        return moveItem( from, fromSlot - 1, to, toSlot.orElse( 0 ) - 1, actualLimit );
    }

    @Nullable
    private static IItemHandler extractHandler( @Nonnull Object object )
    {
        if( object instanceof ICapabilityProvider )
        {
            LazyOptional<IItemHandler> cap = ((ICapabilityProvider) object).getCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY );
            if( cap.isPresent() ) return cap.orElseThrow( NullPointerException::new );
        }

        if( object instanceof IItemHandler ) return (IItemHandler) object;
        if( object instanceof IInventory ) return new InvWrapper( (IInventory) object );
        return null;
    }

    @Nonnull
    private static <T extends Map<? super String, Object>> T fillBasicMeta( @Nonnull T data, @Nonnull ItemStack stack )
    {
        data.put( "name", Objects.toString( stack.getItem().getRegistryName() ) );
        data.put( "count", stack.getCount() );
        return data;
    }

    /**
     * Move an item from one handler to another.
     *
     * @param from     The handler to move from.
     * @param fromSlot The slot to move from.
     * @param to       The handler to move to.
     * @param toSlot   The slot to move to. Use any number < 0 to represent any slot.
     * @param limit    The max number to move. {@link Integer#MAX_VALUE} for no limit.
     * @return The number of items moved.
     */
    private static int moveItem( IItemHandler from, int fromSlot, IItemHandler to, int toSlot, final int limit )
    {
        // See how much we can get out of this slot
        ItemStack extracted = from.extractItem( fromSlot, limit, true );
        if( extracted.isEmpty() ) return 0;

        // Limit the amount to extract
        int extractCount = Math.min( extracted.getCount(), limit );
        extracted.setCount( extractCount );

        ItemStack remainder = toSlot < 0 ? ItemHandlerHelper.insertItem( to, extracted, false ) : to.insertItem( toSlot, extracted, false );
        int inserted = remainder.isEmpty() ? extractCount : extractCount - remainder.getCount();
        if( inserted <= 0 ) return 0;

        // Remove the item from the original inventory. Technically this could fail, but there's little we can do
        // about that.
        from.extractItem( fromSlot, inserted, false );
        return inserted;
    }
}
