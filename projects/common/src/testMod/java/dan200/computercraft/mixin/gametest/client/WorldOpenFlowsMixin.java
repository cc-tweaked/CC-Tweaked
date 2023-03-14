/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.gametest.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {
    /**
     * Never prompt for backup/experimental options when running tests.
     *
     * @param screen     The current menu.
     * @param level      The level to load.
     * @param customised Whether this rule uses legacy customised worldgen options.
     * @param action     The action run to load the world.
     * @author SquidDev
     * @reason Makes it easier to run tests. We can switch to an @Inject if this becomes a problem.
     */
    @Overwrite
    @SuppressWarnings("UnusedMethod")
    private void askForBackup(Screen screen, String level, boolean customised, Runnable action) {
        action.run();
    }
}
