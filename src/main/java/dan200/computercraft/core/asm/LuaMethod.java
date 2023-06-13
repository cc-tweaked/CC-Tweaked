// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.IArguments;


public interface LuaMethod {
    Object[] apply(Object target, IArguments args) throws Exception;
}
