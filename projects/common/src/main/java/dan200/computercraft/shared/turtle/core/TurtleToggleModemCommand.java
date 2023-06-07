package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlockEntity;

public class TurtleToggleModemCommand implements TurtleCommand {
    private final InteractDirection direction;

    public TurtleToggleModemCommand(InteractDirection direction) {
        this.direction = direction;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {

        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Get entity for modem in front
        var world = turtle.getLevel();
        var turtlePosition = turtle.getPosition();
        var modemPosition = turtlePosition.relative(direction);
        var entity = world.getBlockEntity(modemPosition);

        // Use modem
        if (entity instanceof WiredModemFullBlockEntity wiredModemEntity) {
            wiredModemEntity.use();
            return TurtleCommandResult.success();
        } else {
            return TurtleCommandResult.failure("No modem found");
        }

    }
}
