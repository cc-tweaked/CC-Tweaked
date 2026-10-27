// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class TurtleToolCommand implements TurtleCommand {
    private final TurtleVerb verb;
    private final InteractDirection direction;
    private final @Nullable TurtleSide side;

    public TurtleToolCommand(TurtleVerb verb, InteractDirection direction, @Nullable TurtleSide side) {
        this.verb = verb;
        this.direction = direction;
        this.side = side;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TurtleToolCommand.class);

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        TurtleCommandResult firstFailure = null;
        for (var side : TurtleSide.values()) {
            if (this.side != null && this.side != side) continue;

            LOG.info("[{}] Dig {} => {}", this, side, turtle.isRemoved());

            var upgrade = turtle.getUpgrade(side);
            if (upgrade == null || !upgrade.getUpgrade().getUpgradeType().isTool()) continue;

            var result = upgrade.getUpgrade().useTool(turtle, side, upgrade, verb, direction.toWorldDir(turtle));
            if (result.isSuccess()) {
                switch (side) {
                    case LEFT -> turtle.playAnimation(TurtleAnimation.SWING_LEFT_TOOL);
                    case RIGHT -> turtle.playAnimation(TurtleAnimation.SWING_RIGHT_TOOL);
                    default -> turtle.playAnimation(TurtleAnimation.WAIT);
                }
                return result;
            } else if (firstFailure == null) {
                firstFailure = result;
            }

            // If the turtle has been broken by the previous tool interaction, abort.
            if (turtle.isRemoved()) break;
        }
        return firstFailure != null ? firstFailure
            : TurtleCommandResult.failure("No tool to " + verb.name().toLowerCase(Locale.ROOT) + " with");
    }

    public static TurtleToolCommand attack(InteractDirection direction, @Nullable TurtleSide side) {
        return new TurtleToolCommand(TurtleVerb.ATTACK, direction, side);
    }

    public static TurtleToolCommand dig(InteractDirection direction, @Nullable TurtleSide side) {
        return new TurtleToolCommand(TurtleVerb.DIG, direction, side);
    }
}
