// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.config.Config;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Expands a monitor into available space. This tries to expand in each direction until a fixed point is reached.
 */
class Expander {
    private static final Logger LOG = LoggerFactory.getLogger(Expander.class);

    private final Level level;
    private final Direction down;
    private final Direction right;

    private MonitorBlockEntity origin;
    private int width;
    private int height;

    Expander(MonitorBlockEntity origin) {
        this.origin = origin;
        width = origin.getWidth();
        height = origin.getHeight();

        level = Objects.requireNonNull(origin.getLevel(), "level cannot be null");
        down = origin.getDown();
        right = origin.getRight();
    }

    void expand() {
        var changedCount = 0;

        // Impose a limit on the number of resizes we can attempt. There's a risk of getting into an infinite loop
        // if we merge right/down and the next monitor has a width/height of 0. This /should/ never happen - validation
        // will catch it - but I also have a complete lack of faith in the code.
        // As an aside, I think the actual limit is width+height resizes, but again - complete lack of faith.
        var changeLimit = Config.monitorWidth * Config.monitorHeight + 1;
        while (expandIn(true, false) || expandIn(true, true) ||
            expandIn(false, false) || expandIn(false, true)
        ) {
            changedCount++;
            if (changedCount > changeLimit) {
                LOG.error("Monitor has grown too much. This suggests there's an empty monitor in the world.");
                break;
            }
        }

        if (changedCount > 0) origin.resize(width, height);
    }

    /**
     * Attempt to expand a monitor in a particular direction as much as possible.
     *
     * @param useXAxis   {@literal true} if we're expanding on the X Axis, {@literal false} if on the Y.
     * @param isPositive {@literal true} if we're expanding in the positive direction, {@literal false} if negative.
     * @return If the monitor changed.
     */
    private boolean expandIn(boolean useXAxis, boolean isPositive) {
        var pos = origin.getBlockPos();
        int height = this.height, width = this.width;

        var otherOffset = isPositive ? (useXAxis ? width : height) : -1;
        var otherPos = useXAxis ? pos.relative(right, otherOffset) : pos.relative(down, otherOffset);
        var other = level.getBlockEntity(otherPos);
        if (!(other instanceof MonitorBlockEntity otherMonitor) || !origin.isCompatible(otherMonitor)) return false;

        if (useXAxis) {
            if (otherMonitor.getYIndex() != 0 || otherMonitor.getHeight() != height) return false;
            width += otherMonitor.getWidth();
            if (width > Config.monitorWidth) return false;
        } else {
            if (otherMonitor.getXIndex() != 0 || otherMonitor.getWidth() != width) return false;
            height += otherMonitor.getHeight();
            if (height > Config.monitorHeight) return false;
        }

        if (!isPositive) {
            var otherOrigin = level.getBlockEntity(otherMonitor.toWorldPos(0, 0));
            if (!(otherOrigin instanceof MonitorBlockEntity originMonitor) || !origin.isCompatible(originMonitor)) {
                return false;
            }

            origin = originMonitor;
        }

        this.width = width;
        this.height = height;

        return true;
    }

}
