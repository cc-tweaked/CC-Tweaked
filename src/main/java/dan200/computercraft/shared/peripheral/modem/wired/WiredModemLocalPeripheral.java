/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.Peripherals;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a local peripheral exposed on the wired network.
 *
 * This is responsible for getting the peripheral in world, tracking id and type and determining whether it has changed.
 */
public final class WiredModemLocalPeripheral
{
    private static final String NBT_PERIPHERAL_TYPE = "PeripheralType";
    private static final String NBT_PERIPHERAL_ID = "PeripheralId";

    private int id;
    private String type;

    private IPeripheral peripheral;

    /**
     * Attach a new peripheral from the world.
     *
     * @param world     The world to search in
     * @param origin    The position to search from
     * @param direction The direction so search in
     * @return Whether the peripheral changed.
     */
    public boolean attach( @Nonnull World world, @Nonnull BlockPos origin, @Nonnull Direction direction )
    {
        IPeripheral oldPeripheral = peripheral;
        IPeripheral peripheral = this.peripheral = getPeripheralFrom( world, origin, direction );

        if( peripheral == null )
        {
            return oldPeripheral != null;
        }
        else
        {
            String type = peripheral.getType();
            int id = this.id;

            if( id > 0 && this.type == null )
            {
                // If we had an ID but no type, then just set the type.
                this.type = type;
            }
            else if( id < 0 || !type.equals( this.type ) )
            {
                this.type = type;
                this.id = IDAssigner.getNextId( "peripheral." + type );
            }

            return oldPeripheral == null || !oldPeripheral.equals( peripheral );
        }
    }

    @Nullable
    private IPeripheral getPeripheralFrom( World world, BlockPos pos, Direction direction )
    {
        BlockPos offset = pos.offset( direction );

        Block block = world.getBlockState( offset )
            .getBlock();
        if( block == ComputerCraftRegistry.ModBlocks.WIRED_MODEM_FULL || block == ComputerCraftRegistry.ModBlocks.CABLE )
        {
            return null;
        }

        IPeripheral peripheral = Peripherals.getPeripheral( world, offset, direction.getOpposite() );
        return peripheral instanceof WiredModemPeripheral ? null : peripheral;
    }

    /**
     * Detach the current peripheral.
     *
     * @return Whether the peripheral changed
     */
    public boolean detach()
    {
        if( peripheral == null )
        {
            return false;
        }
        peripheral = null;
        return true;
    }

    @Nullable
    public String getConnectedName()
    {
        return peripheral != null ? type + "_" + id : null;
    }

    @Nullable
    public IPeripheral getPeripheral()
    {
        return peripheral;
    }

    public boolean hasPeripheral()
    {
        return peripheral != null;
    }

    public void extendMap( @Nonnull Map<String, IPeripheral> peripherals )
    {
        if( peripheral != null )
        {
            peripherals.put( type + "_" + id, peripheral );
        }
    }

    public Map<String, IPeripheral> toMap()
    {
        return peripheral == null ? Collections.emptyMap() : Collections.singletonMap( type + "_" + id, peripheral );
    }

    public void write( @Nonnull CompoundTag tag, @Nonnull String suffix )
    {
        if( id >= 0 )
        {
            tag.putInt( NBT_PERIPHERAL_ID + suffix, id );
        }
        if( type != null )
        {
            tag.putString( NBT_PERIPHERAL_TYPE + suffix, type );
        }
    }

    public void read( @Nonnull CompoundTag tag, @Nonnull String suffix )
    {
        id = tag.contains( NBT_PERIPHERAL_ID + suffix, NBTUtil.TAG_ANY_NUMERIC ) ? tag.getInt( NBT_PERIPHERAL_ID + suffix ) : -1;

        type = tag.contains( NBT_PERIPHERAL_TYPE + suffix, NBTUtil.TAG_STRING ) ? tag.getString( NBT_PERIPHERAL_TYPE + suffix ) : null;
    }
}
