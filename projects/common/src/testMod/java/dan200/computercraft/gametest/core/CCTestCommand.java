// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import com.mojang.brigadier.CommandDispatcher;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.mixin.gametest.TestCommandAccessor;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static dan200.computercraft.core.util.Nullability.assertNonNull;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.choice;
import static net.minecraft.commands.Commands.literal;

/**
 * Helper commands for importing/exporting the computer directory.
 */
class CCTestCommand {
    public static final LevelResource LOCATION = new LevelResource(ComputerCraftAPI.MOD_ID);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(choice("cctest")
            .then(literal("import").executes(context -> {
                importFiles(context.getSource().getServer());
                return 0;
            }))
            .then(literal("export").executes(context -> {
                exportFiles(context.getSource().getServer());

                for (var function : GameTestRegistry.getAllTestFunctions()) {
                    TestCommandAccessor.callExportTestStructure(context.getSource(), function.getStructureName());
                }
                return 0;
            }))
            .then(literal("regen-structures").executes(context -> {
                for (var function : GameTestRegistry.getAllTestFunctions()) {
                    dispatcher.execute("test import " + function.getTestName(), context.getSource());
                    TestCommandAccessor.callExportTestStructure(context.getSource(), function.getStructureName());
                }
                return 0;
            }))

            .then(literal("marker").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var pos = StructureUtils.findNearestStructureBlock(player.blockPosition(), 15, player.serverLevel());
                if (pos == null) return error(context.getSource(), "No nearby test");

                var structureBlock = (StructureBlockEntity) player.level().getBlockEntity(pos);
                if (structureBlock == null) return error(context.getSource(), "No nearby structure block");
                var info = GameTestRegistry.getTestFunction(structureBlock.getStructurePath());

                // Kill the existing armor stand
                player
                    .serverLevel().getEntities(EntityType.ARMOR_STAND, x -> x.isAlive() && x.getName().getString().equals(info.getTestName()))
                    .forEach(Entity::kill);

                // And create a new one
                var nbt = new CompoundTag();
                nbt.putBoolean("Marker", true);
                nbt.putBoolean("Invisible", true);
                var armorStand = assertNonNull(EntityType.ARMOR_STAND.create(player.level()));
                armorStand.readAdditionalSaveData(nbt);
                armorStand.copyPosition(player);
                armorStand.setCustomName(Component.literal(info.getTestName()));
                player.level().addFreshEntity(armorStand);
                return 0;
            }))

            .then(literal("give-computer").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var pos = StructureUtils.findNearestStructureBlock(player.blockPosition(), 15, player.serverLevel());
                if (pos == null) return error(context.getSource(), "No nearby test");

                var structureBlock = (StructureBlockEntity) player.level().getBlockEntity(pos);
                if (structureBlock == null) return error(context.getSource(), "No nearby structure block");
                var info = GameTestRegistry.getTestFunction(structureBlock.getStructurePath());

                var item = ModRegistry.Items.COMPUTER_ADVANCED.get().create(1, info.getTestName());
                if (!player.getInventory().add(item)) {
                    var itemEntity = player.drop(item, false);
                    if (itemEntity != null) {
                        itemEntity.setNoPickUpDelay();
                        itemEntity.setThrower(player.getUUID());
                    }
                }

                return 1;
            }))
        );
    }

    public static void importFiles(MinecraftServer server) {
        try {
            Copier.replicate(getSourceComputerPath(), getWorldComputerPath(server));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void exportFiles(MinecraftServer server) {
        try {
            Copier.replicate(getWorldComputerPath(server), getSourceComputerPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path getWorldComputerPath(MinecraftServer server) {
        return server.getWorldPath(LOCATION).resolve("computer").resolve("0");
    }

    private static Path getSourceComputerPath() {
        return TestHooks.getSourceDir().resolve("computer");
    }

    private static int error(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }
}
