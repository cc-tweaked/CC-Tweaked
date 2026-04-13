// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAt;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A {@link DebugScreenEntry} that provides information about the currently looked at block entity.
 */
public final class LookingAtBlockEntityDebugEntry extends DebugEntryLookingAt {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "looking_at_block_entity");

    private final Map<BlockEntityType<?>, BiConsumer<List<String>, BlockEntity>> blockEntityEmitters = new HashMap<>();

    private LookingAtBlockEntityDebugEntry() {
    }

    @Override
    public HitResult getHitResult(Entity camera) {
        return camera.pick(20.0, 0.0F, false);
    }

    @Override
    public void extractInfo(List<String> result, Level level, BlockPos pos) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return;

        var emitter = blockEntityEmitters.get(blockEntity.getType());
        if (emitter == null) return;

        emitter.accept(result, blockEntity);
    }

    @Override
    public Identifier group() {
        return ID;
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
