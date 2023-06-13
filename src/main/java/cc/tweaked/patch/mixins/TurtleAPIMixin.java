// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package cc.tweaked.patch.mixins;

import cc.tweaked.patch.framework.transform.MergeVisitor;
import dan200.CCTurtle;
import dan200.computer.core.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.turtle.shared.ITurtle;

/**
 * Adds additional methods to {@link dan200.turtle.shared.TurtleAPI}.
 */
public abstract class TurtleAPIMixin implements ILuaAPI {
    @MergeVisitor.Shadow
    private ITurtle m_turtle;

    /**
     * Get the currently selected slot.
     *
     * @return The current slot.
     * @cc.since 1.6
     */
    @LuaFunction
    public final int getSelectedSlot() {
        return m_turtle.getSelectedSlot() + 1;
    }

    /**
     * Get the maximum amount of fuel this turtle can hold.
     * <p>
     * By default, normal turtles have a limit of 20,000 and advanced turtles of 100,000.
     *
     * @return The limit, or "unlimited".
     * @cc.treturn [1] number The maximum amount of fuel a turtle can hold.
     * @cc.treturn [2] "unlimited" If turtles do not consume fuel when moving.
     * @cc.since 1.6
     */
    @LuaFunction
    public final Object getFuelLimit() {
        return CCTurtle.turtlesNeedFuel ? Integer.MAX_VALUE : "unlimited";
    }
}
