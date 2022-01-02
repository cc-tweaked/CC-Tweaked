/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.ComputerCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Expands a monitor into available space. This tries to expand in each direction until a fixed point is reached.
 */
class Expander
{
    private final Level level;
    private final Direction down;
    private final Direction right;

    private TileMonitor origin;
    private int width;
    private int height;

    Expander( TileMonitor origin )
    {
        this.origin = origin;
        width = origin.getWidth();
        height = origin.getHeight();

        level = Objects.requireNonNull( origin.getLevel(), "level cannot be null" );
        down = origin.getDown();
        right = origin.getRight();
    }

    void expand()
    {
        int changedCount = 0;

        // Impose a limit on the number of resizes we can attempt. There's a risk of getting into an infinite loop
        // if we merge right/down and the next monitor has a width/height of 0. This /should/ never happen - validation
        // will catch it - but I also have a complete lack of faith in the code.
        // As an aside, I think the actual limit is width+height resizes, but again - complete lack of faith.
        int changeLimit = ComputerCraft.monitorWidth * ComputerCraft.monitorHeight + 1;
        while( expandIn( true, false ) || expandIn( true, true ) ||
            expandIn( false, false ) || expandIn( false, true )
        )
        {
            changedCount++;
            if( changedCount > changeLimit )
            {
                ComputerCraft.log.error( "Monitor has grown too much. This suggests there's an empty monitor in the world." );
                break;
            }
        }

        if( changedCount > 0 ) origin.resize( width, height );
    }

    /**
     * Attempt to expand a monitor in a particular direction as much as possible.
     *
     * @param useXAxis   {@literal true} if we're expanding on the X Axis, {@literal false} if on the Y.
     * @param isPositive {@literal true} if we're expanding in the positive direction, {@literal false} if negative.
     * @return If the monitor changed.
     */
    private boolean expandIn( boolean useXAxis, boolean isPositive )
    {
        BlockPos pos = origin.getBlockPos();
        int height = this.height, width = this.width;

        int otherOffset = isPositive ? (useXAxis ? width : height) : -1;
        BlockPos otherPos = useXAxis ? pos.relative( right, otherOffset ) : pos.relative( down, otherOffset );
        BlockEntity other = level.getBlockEntity( otherPos );
        if( !(other instanceof TileMonitor otherMonitor) || !origin.isCompatible( otherMonitor ) ) return false;

        if( useXAxis )
        {
            if( otherMonitor.getYIndex() != 0 || otherMonitor.getHeight() != height ) return false;
            width += otherMonitor.getWidth();
            if( width > ComputerCraft.monitorWidth ) return false;
        }
        else
        {
            if( otherMonitor.getXIndex() != 0 || otherMonitor.getWidth() != width ) return false;
            height += otherMonitor.getHeight();
            if( height > ComputerCraft.monitorHeight ) return false;
        }

        if( !isPositive )
        {
            BlockEntity otherOrigin = level.getBlockEntity( otherMonitor.toWorldPos( 0, 0 ) );
            if( !(otherOrigin instanceof TileMonitor originMonitor) || !origin.isCompatible( originMonitor ) )
            {
                return false;
            }

            origin = originMonitor;
        }

        this.width = width;
        this.height = height;

        return true;
    }

}
