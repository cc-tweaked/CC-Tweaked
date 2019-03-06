/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import net.minecraft.util.StringRepresentable;
import net.minecraft.util.math.Direction;

import javax.annotation.Nonnull;

public enum CableModemVariant implements StringRepresentable
{
    None( "none", null ),
    DownOff( "down_off", Direction.DOWN ),
    UpOff( "up_off", Direction.UP ),
    NorthOff( "north_off", Direction.NORTH ),
    SouthOff( "south_off", Direction.SOUTH ),
    WestOff( "west_off", Direction.WEST ),
    EastOff( "east_off", Direction.EAST ),
    DownOn( "down_on", Direction.DOWN ),
    UpOn( "up_on", Direction.UP ),
    NorthOn( "north_on", Direction.NORTH ),
    SouthOn( "south_on", Direction.SOUTH ),
    WestOn( "west_on", Direction.WEST ),
    EastOn( "east_on", Direction.EAST ),
    DownOffPeripheral( "down_off_peripheral", Direction.DOWN ),
    UpOffPeripheral( "up_off_peripheral", Direction.UP ),
    NorthOffPeripheral( "north_off_peripheral", Direction.NORTH ),
    SouthOffPeripheral( "south_off_peripheral", Direction.SOUTH ),
    WestOffPeripheral( "west_off_peripheral", Direction.WEST ),
    EastOffPeripheral( "east_off_peripheral", Direction.EAST ),
    DownOnPeripheral( "down_on_peripheral", Direction.DOWN ),
    UpOnPeripheral( "up_on_peripheral", Direction.UP ),
    NorthOnPeripheral( "north_on_peripheral", Direction.NORTH ),
    SouthOnPeripheral( "south_on_peripheral", Direction.SOUTH ),
    WestOnPeripheral( "west_on_peripheral", Direction.WEST ),
    EastOnPeripheral( "east_on_peripheral", Direction.EAST );

    private static final CableModemVariant[] VALUES = values();

    public static CableModemVariant from( Direction facing )
    {
        return facing == null ? None : VALUES[1 + facing.ordinal()];
    }

    public static CableModemVariant from( Direction facing, boolean modem, boolean peripheral )
    {
        int state = (modem ? 2 : 0) + (peripheral ? 1 : 0);
        return facing == null ? None : VALUES[1 + 6 * state + facing.ordinal()];
    }

    private final String m_name;
    private final Direction m_facing;

    CableModemVariant( String name, Direction facing )
    {
        m_name = name;
        m_facing = facing;
    }

    @Nonnull
    @Override
    public String asString()
    {
        return m_name;
    }

    public Direction getFacing()
    {
        return m_facing;
    }

    @Override
    public String toString()
    {
        return asString();
    }
}
