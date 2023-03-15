// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.*;

import javax.annotation.Nullable;
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

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        TurtleCommandResult firstFailure = null;
        for (var side : TurtleSide.values()) {
            if (this.side != null && this.side != side) continue;

            var upgrade = turtle.getUpgrade(side);
            if (upgrade == null || !upgrade.getType().isTool()) continue;

            var result = upgrade.useTool(turtle, side, verb, direction.toWorldDir(turtle));
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
