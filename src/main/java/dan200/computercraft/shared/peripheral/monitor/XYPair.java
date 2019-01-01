/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

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

    public static XYPair of( float xPos, float yPos, float zPos, int side )
    {
        switch( side )
        {
            case 2:
                return new XYPair( 1 - xPos, 1 - yPos );
            case 3:
                return new XYPair( xPos, 1 - yPos );
            case 4:
                return new XYPair( zPos, 1 - yPos );
            case 5:
                return new XYPair( 1 - zPos, 1 - yPos );
            case 8:
                return new XYPair( 1 - xPos, zPos );
            case 9:
                return new XYPair( xPos, 1 - zPos );
            case 10:
                return new XYPair( zPos, xPos );
            case 11:
                return new XYPair( 1 - zPos, 1 - xPos );
            case 14:
                return new XYPair( 1 - xPos, 1 - zPos );
            case 15:
                return new XYPair( xPos, zPos );
            case 16:
                return new XYPair( zPos, 1 - xPos );
            case 17:
                return new XYPair( 1 - zPos, xPos );
            default:
                return new XYPair( xPos, zPos );
        }
    }
}
