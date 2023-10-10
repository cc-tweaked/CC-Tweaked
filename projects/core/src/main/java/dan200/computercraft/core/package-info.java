// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

/**
 * ComputerCraft's core Lua runtime and APIs.
 * <p>
 * This is not considered part of the stable API, and so should not be consumed by other Minecraft mods. However,
 * emulators or other CC-tooling may find this useful.
 */
@DefaultQualifier(value = NonNull.class, locations = {
    TypeUseLocation.RETURN,
    TypeUseLocation.PARAMETER,
    TypeUseLocation.FIELD,
})
package dan200.computercraft.core;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
