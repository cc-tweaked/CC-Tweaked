// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAtBlock;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A {@link DebugScreenEntry} that provides information about the currently looked at block entity.
 *
 * @see DebugEntryLookingAtBlock
 */
public final class LookingAtBlockEntityDebugEntry implements DebugScreenEntry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "looking_at_block_entity");

    private final Map<BlockEntityType<?>, BiConsumer<List<String>, BlockEntity>> blockEntityEmitters = new HashMap<>();

    private LookingAtBlockEntityDebugEntry() {
    }

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        var entity = Minecraft.getInstance().getCameraEntity();
        var trueLevel = SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES ? level : Minecraft.getInstance().level;
        if (entity == null || trueLevel == null) return;

        var hitResult = entity.pick(20.0, 0.0F, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) return;

        var blockEntity = trueLevel.getBlockEntity(((BlockHitResult) hitResult).getBlockPos());
        if (blockEntity == null) return;
        var emitter = blockEntityEmitters.get(blockEntity.getType());
        if (emitter == null) return;

        List<String> lines = new ArrayList<>();
        emitter.accept(lines, blockEntity);
        displayer.addToGroup(ID, lines);
    }

    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> LookingAtBlockEntityDebugEntry register(BlockEntityType<T> type, BiConsumer<List<String>, T> emit) {
        blockEntityEmitters.put(type, (BiConsumer<List<String>, BlockEntity>) emit);
        return this;
    }

    public static DebugScreenEntry create() {
        return new LookingAtBlockEntityDebugEntry()
            .register(ModRegistry.BlockEntities.MONITOR_NORMAL.get(), LookingAtBlockEntityDebugEntry::debugMonitor)
            .register(ModRegistry.BlockEntities.MONITOR_ADVANCED.get(), LookingAtBlockEntityDebugEntry::debugMonitor)
            .register(ModRegistry.BlockEntities.TURTLE_NORMAL.get(), LookingAtBlockEntityDebugEntry::debugTurtle)
            .register(ModRegistry.BlockEntities.TURTLE_ADVANCED.get(), LookingAtBlockEntityDebugEntry::debugTurtle);
    }

    private static void debugMonitor(List<String> lines, MonitorBlockEntity monitor) {
        lines.add(
            String.format("Targeted monitor: (%d, %d), %d x %d", monitor.getXIndex(), monitor.getYIndex(), monitor.getWidth(), monitor.getHeight())
        );
    }

    private static void debugTurtle(List<String> lines, TurtleBlockEntity turtle) {
        lines.add("Targeted turtle:");
        lines.add(String.format("Id: %d", turtle.getComputerID()));
        addTurtleUpgrade(lines, turtle, TurtleSide.LEFT);
        addTurtleUpgrade(lines, turtle, TurtleSide.RIGHT);
    }

    private static void addTurtleUpgrade(List<String> out, TurtleBlockEntity turtle, TurtleSide side) {
        var upgrade = turtle.getAccess().getUpgradeWithData(side);
        if (upgrade != null) out.add(String.format("Upgrade[%s]: %s", side, upgrade.holder().key().identifier()));
    }

}
