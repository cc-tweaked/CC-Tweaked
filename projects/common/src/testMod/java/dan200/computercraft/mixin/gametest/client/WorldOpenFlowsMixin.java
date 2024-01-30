// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest.client;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {
    /**
     * Never prompt for backup/experimental options when running tests.
     *
     * @param access     The current menu.
     * @param customised Whether this rule uses legacy customised worldgen options.
     * @param load       The action run to load the world.
     * @param cancel     The action run to abort loading the world.
     * @author SquidDev
     * @reason Makes it easier to run tests. We can switch to an @Inject if this becomes a problem.
     */
    @Overwrite
    @SuppressWarnings("UnusedMethod")
    private void askForBackup(LevelStorageSource.LevelStorageAccess access, boolean customised, Runnable load, Runnable cancel) {
        load.run();
    }
}
