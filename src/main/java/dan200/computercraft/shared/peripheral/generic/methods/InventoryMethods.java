/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.methods;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.asm.GenericSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.versions.forge.ForgeVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Nonnull
    public static <T extends Map<? super String, Object>> T fillBasicMeta( @Nonnull T data, @Nonnull ItemStack stack )
    {
        data.put( "name", Objects.toString( stack.getItem().getRegistryName() ) );
        data.put( "count", stack.getCount() );
        return data;
    }
}
