/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum CableModemVariant implements IStringSerializable
{
    None( "none", null ),
    DownOff( "down_off", EnumFacing.DOWN ),
    UpOff( "up_off", EnumFacing.UP ),
    NorthOff( "north_off", EnumFacing.NORTH ),
    SouthOff( "south_off", EnumFacing.SOUTH ),
    WestOff( "west_off", EnumFacing.WEST ),
    EastOff( "east_off", EnumFacing.EAST ),
    DownOn( "down_on", EnumFacing.DOWN ),
    UpOn( "up_on", EnumFacing.UP ),
    NorthOn( "north_on", EnumFacing.NORTH ),
    SouthOn( "south_on", EnumFacing.SOUTH ),
    WestOn( "west_on", EnumFacing.WEST ),
    EastOn( "east_on", EnumFacing.EAST ),
    DownOffPeripheral( "down_off_peripheral", EnumFacing.DOWN ),
    UpOffPeripheral( "up_off_peripheral", EnumFacing.UP ),
    NorthOffPeripheral( "north_off_peripheral", EnumFacing.NORTH ),
    SouthOffPeripheral( "south_off_peripheral", EnumFacing.SOUTH ),
    WestOffPeripheral( "west_off_peripheral", EnumFacing.WEST ),
    EastOffPeripheral( "east_off_peripheral", EnumFacing.EAST ),
    DownOnPeripheral( "down_on_peripheral", EnumFacing.DOWN ),
    UpOnPeripheral( "up_on_peripheral", EnumFacing.UP ),
    NorthOnPeripheral( "north_on_peripheral", EnumFacing.NORTH ),
    SouthOnPeripheral( "south_on_peripheral", EnumFacing.SOUTH ),
    WestOnPeripheral( "west_on_peripheral", EnumFacing.WEST ),
    EastOnPeripheral( "east_on_peripheral", EnumFacing.EAST );

    private static final CableModemVariant[] VALUES = values();

    private final String name;
    private final EnumFacing facing;

    CableModemVariant( String name, EnumFacing facing )
    {
        this.name = name;
        this.facing = facing;
    }

    @Nonnull
    public static CableModemVariant from( EnumFacing facing )
    {
        return facing == null ? None : VALUES[1 + facing.getIndex()];
    }

    @Nonnull
    public static CableModemVariant from( EnumFacing facing, boolean modem, boolean peripheral )
    {
        int state = (modem ? 2 : 0) + (peripheral ? 1 : 0);
        return facing == null ? None : VALUES[1 + 6 * state + facing.getIndex()];
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    public EnumFacing getFacing()
    {
        return facing;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
