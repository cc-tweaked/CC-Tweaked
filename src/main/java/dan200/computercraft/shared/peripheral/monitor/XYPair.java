/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import net.minecraft.core.Direction;

public class XYPair
{
    public final float x;
    public final float y;

    public XYPair( float x, float y )
    {
        this.x = x;
        this.y = y;
    }

    public XYPair add( float x, float y )
    {
        return new XYPair( this.x + x, this.y + y );
    }

    public static XYPair of( float xPos, float yPos, float zPos, Direction facing, Direction orientation )
    {
        switch( orientation )
        {
            case NORTH:
                switch( facing )
                {
                    case NORTH:
                        return new XYPair( 1 - xPos, 1 - yPos );
                    case SOUTH:
                        return new XYPair( xPos, 1 - yPos );
                    case WEST:
                        return new XYPair( zPos, 1 - yPos );
                    case EAST:
                        return new XYPair( 1 - zPos, 1 - yPos );
                }
                break;
            case DOWN:
                switch( facing )
                {
                    case NORTH:
                        return new XYPair( 1 - xPos, zPos );
                    case SOUTH:
                        return new XYPair( xPos, 1 - zPos );
                    case WEST:
                        return new XYPair( zPos, xPos );
                    case EAST:
                        return new XYPair( 1 - zPos, 1 - xPos );
                }
                break;
            case UP:
                switch( facing )
                {
                    case NORTH:
                        return new XYPair( 1 - xPos, 1 - zPos );
                    case SOUTH:
                        return new XYPair( xPos, zPos );
                    case WEST:
                        return new XYPair( zPos, 1 - xPos );
                    case EAST:
                        return new XYPair( 1 - zPos, xPos );
                }
                break;
        }

        return new XYPair( xPos, zPos );
    }
}
