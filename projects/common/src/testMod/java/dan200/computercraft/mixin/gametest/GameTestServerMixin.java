// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import com.mojang.datafixers.DataFixer;
import dan200.computercraft.gametest.core.TestHooks;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;

@Mixin(GameTestServer.class)
abstract class GameTestServerMixin extends MinecraftServer {
    GameTestServerMixin(
        Thread serverThread, LevelStorageSource.LevelStorageAccess storageSource, PackRepository packRepository,
        WorldStem worldStem, Optional<GameRules> gameRules, Proxy proxy, DataFixer fixerUpper, Services services,
        LevelLoadListener progressListenerFactory, boolean propagatesCrashes, NotificationManager notificationManager
    ) {
        super(serverThread, storageSource, packRepository, worldStem, gameRules, proxy, fixerUpper, services, progressListenerFactory, propagatesCrashes, notificationManager);
    }

    /**
     * Overwrite {@link GameTestServer#waitUntilNextTick()} to wait for all computers to finish executing.
     * <p>
     * This is a little dangerous (breaks async behaviour of computers), but it forces tests to be deterministic.
     *
     * @reason See above. This is only in the test mod, so no risk of collision.
     * @author SquidDev.
     */
    @Overwrite
    @Override
    public void waitUntilNextTick() {
        while (true) {
            runAllTasks();
            if (!haveTestsStarted() || TestHooks.areComputersIdle(this)) break;
            LockSupport.parkNanos(100_000);
        }
    }

    @Shadow
    private boolean haveTestsStarted() {
        throw new AssertionError("Stub.");
    }
}
