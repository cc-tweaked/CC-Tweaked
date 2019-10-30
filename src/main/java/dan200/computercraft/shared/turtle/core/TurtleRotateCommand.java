package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public class TurtleRotateCommand implements ITurtleCommand {

    private final InteractDirection direction;
    private final Direction target_direction;

    public TurtleRotateCommand( InteractDirection direction, Direction target_direction )
    {
        this.direction = direction;
        this.target_direction = target_direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Get world direction from direction
        Direction direction = this.direction.toWorldDir( turtle );

        // Check if thing in front is air or not
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );

        BlockState state = world.getBlockState( newPosition );
        if( state.isAir() )
        {
            return TurtleCommandResult.failure( "No block to rotate" );
        }

        world.setBlockState(newPosition, state.with(Properties.FACING, this.target_direction));
        world.updateNeighbor(newPosition, state.getBlock(), newPosition);

        // Fire the event, exiting if it is cancelled
        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleBlockEvent.Rotate event = new TurtleBlockEvent.Rotate( turtle, turtlePlayer, world, newPosition, state, this.target_direction );
        if( MinecraftForge.EVENT_BUS.post( event ) ) return TurtleCommandResult.failure( event.getFailureMessage() );

        return TurtleCommandResult.success();
    }
}
