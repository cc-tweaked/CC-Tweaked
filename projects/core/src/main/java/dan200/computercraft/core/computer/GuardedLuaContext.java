// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;

/**
 * A {@link ILuaContext} which checks if context is valid when before executing
 * {@linkplain #issueMainThreadTask(LuaTask) main-thread tasks}.
 */
public final class GuardedLuaContext implements ILuaContext {
    private final ILuaContext original;
    private final Guard guard;

    public GuardedLuaContext(ILuaContext original, Guard guard) {
        this.original = original;
        this.guard = guard;
    }

    /**
     * Determine if this {@link GuardedLuaContext} wraps another context.
     * <p>
     * This may be used to avoid constructing new guarded contexts, in a pattern something like:
     *
     * <pre>{@code
     * var contextWrapper = this.contextWrapper;
     * if(contextWrapper == null || !contextWrapper.wraps(context)) {
     *     contextWrapper = this.contextWrapper = new GuardedLuaContext(context, this);
     * }
     * }</pre>
     *
     * @param context The original context.
     * @return Whether {@code this} wraps {@code context}.
     */
    public boolean wraps(ILuaContext context) {
        return original == context;
    }

    @Override
    public long issueMainThreadTask(LuaTask task) throws LuaException {
        return original.issueMainThreadTask(() -> guard.checkValid() ? task.execute() : null);
    }

    /**
     * The function which checks if the context is still valid.
     */
    @FunctionalInterface
    public interface Guard {
        boolean checkValid();
    }
}
