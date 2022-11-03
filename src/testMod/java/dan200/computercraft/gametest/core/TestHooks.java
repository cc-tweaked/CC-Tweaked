/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.gametest.api.Times;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHooks {
    public static final Logger LOGGER = LoggerFactory.getLogger(TestHooks.class);

    public static final Path sourceDir = Paths.get(System.getProperty("cctest.sources")).normalize().toAbsolutePath();

    public static void init() {
        ServerContext.luaMachine = ManagedComputers.INSTANCE;
        ComputerCraftAPI.registerAPIFactory(TestAPI::new);
        StructureUtils.testStructuresDir = sourceDir.resolve("structures").toString();
    }

    public static void onServerStarted(MinecraftServer server) {
        var rules = server.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);

        var world = server.getLevel(Level.OVERWORLD);
        if (world != null) world.setDayTime(Times.NOON);

        LOGGER.info("Cleaning up after last run");
        GameTestRunner.clearAllTests(server.overworld(), new BlockPos(0, -60, 0), GameTestTicker.SINGLETON, 200);

        // Delete server context and add one with a mutable machine factory. This allows us to set the factory for
        // specific test batches without having to reset all computers.
        for (var computer : ServerContext.get(server).registry().getComputers()) {
            var label = computer.getLabel() == null ? "#" + computer.getID() : computer.getLabel();
            LOGGER.warn("Unexpected computer {}", label);
        }

        LOGGER.info("Importing files");
        CCTestCommand.importFiles(server);
    }
}
