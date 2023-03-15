// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

/**
 * A continuation which is called when this coroutine is resumed.
 *
 * @see MethodResult#yield(Object[], ILuaCallback)
 */
public interface ILuaCallback {
    /**
     * Resume this coroutine.
     *
     * @param args The result of resuming this coroutine. These will have the same form as described in
     *             {@link LuaFunction}.
     * @return The result of this continuation. Either the result to return to the callee, or another yield.
     * @throws LuaException On an error.
     */
    MethodResult resume(Object[] args) throws LuaException;
}
