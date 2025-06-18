// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import com.mojang.brigadier.CommandDispatcher;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.mixin.gametest.ArmorStandAccessor;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static dan200.computercraft.shared.command.builder.CommandBuilder.command;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.choice;
import static net.minecraft.commands.Commands.literal;

/**
 * Helper commands for importing/exporting the computer directory.
 */
class CCTestCommand {
    public static final LevelResource LOCATION = new LevelResource(ComputerCraftAPI.MOD_ID);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(choice("cctest")
            .then(literal("marker").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var pos = StructureUtils.findNearestTest(player.blockPosition(), 15, player.level()).orElse(null);
                if (pos == null) return error(context.getSource(), "No nearby test");

                var test = player.level().getBlockEntity(pos, BlockEntityType.TEST_INSTANCE_BLOCK)
                    .flatMap(TestInstanceBlockEntity::test).orElse(null);
                if (test == null) return error(context.getSource(), "No nearby structure block");

                // Kill the existing armor stand
                var level = player.level();
                level.getEntities(EntityType.ARMOR_STAND, x -> x.isAlive() && x.getName().getString().equals(test.location().getPath()))
                    .forEach(e -> e.kill(level));

                // And create a new one
                var nbt = new CompoundTag();
                nbt.putBoolean("Marker", true);
                nbt.putBoolean("Invisible", true);
                var armorStand = new ArmorStand(EntityType.ARMOR_STAND, level);
                armorStand.setInvisible(true);
                ((ArmorStandAccessor) armorStand).computercraft$setMarker(true);
                armorStand.copyPosition(player);
                armorStand.setCustomName(Component.literal(test.location().getPath()));
                level.addFreshEntity(armorStand);
                return 0;
            }))

            .then(command("give-computer").arg("item", ItemArgument.item(buildContext)).executes(context -> {
                var item = context.getArgument("item", ItemInput.class);

                var player = context.getSource().getPlayerOrException();
                var pos = StructureUtils.findNearestTest(player.blockPosition(), 15, player.level()).orElse(null);
                if (pos == null) return error(context.getSource(), "No nearby test");

                var test = player.level().getBlockEntity(pos, BlockEntityType.TEST_INSTANCE_BLOCK)
                    .flatMap(TestInstanceBlockEntity::test).orElse(null);
                if (test == null) return error(context.getSource(), "No nearby structure block");

                var stack = item.createItemStack(1, false);
                stack.set(ModRegistry.DataComponents.COMPUTER_ID.get(), new NonNegativeId.Computer(1));
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(test.location().getPath()));
                if (!player.getInventory().add(stack)) {
                    var itemEntity = player.drop(stack, false);
                    if (itemEntity != null) {
                        itemEntity.setNoPickUpDelay();
                        itemEntity.setThrower(player);
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
