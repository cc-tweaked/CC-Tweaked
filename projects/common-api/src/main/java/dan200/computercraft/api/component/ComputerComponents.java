// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.component;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;

/**
 * The {@link ComputerComponent}s provided by ComputerCraft.
 */
public class ComputerComponents {
    /**
     * The {@link ITurtleAccess} associated with a turtle.
     */
    public static final ComputerComponent<ITurtleAccess> TURTLE = ComputerComponent.create(ComputerCraftAPI.MOD_ID, "turtle");

    /**
     * The {@link IPocketAccess} associated with a pocket computer.
     */
    public static final ComputerComponent<IPocketAccess> POCKET = ComputerComponent.create(ComputerCraftAPI.MOD_ID, "pocket");

    /**
     * This component is only present on "command computers", and other computers with admin capabilities.
     */
    public static final ComputerComponent<AdminComputer> ADMIN_COMPUTER = ComputerComponent.create(ComputerCraftAPI.MOD_ID, "admin_computer");
}
