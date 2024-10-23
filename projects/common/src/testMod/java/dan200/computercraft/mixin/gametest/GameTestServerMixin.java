// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import com.mojang.datafixers.DataFixer;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;

import java.net.Proxy;

@Mixin(GameTestServer.class)
abstract class GameTestServerMixin extends MinecraftServer implements MinecraftServerAccessor {
    GameTestServerMixin(Thread serverThread, LevelStorageSource.LevelStorageAccess storageSource, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer fixerUpper, Services services, ChunkProgressListenerFactory progressListenerFactory) {
        super(serverThread, storageSource, packRepository, worldStem, proxy, fixerUpper, services, progressListenerFactory);
    }

    /**
     * {@link GameTestServer} overrides {@code waitUntilNextTick} to tick as quickly as possible. This does not play
     * well with computers, so we add back {@link MinecraftServer}'s implementation.
     */
    @Override
    public void waitUntilNextTick() {
        runAllTasks();
        computercraft$setWaitingForNextTick(true);

        try {
            this.managedBlock(() -> !computercraft$haveTime());
        } finally {
            computercraft$setWaitingForNextTick(false);
        }
    }
}
