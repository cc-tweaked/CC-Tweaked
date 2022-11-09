/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.BlockGeneric;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public abstract class BlockComputerBase<T extends TileComputerBase> extends BlockGeneric implements IBundledRedstoneBlock {
    private static final ResourceLocation DROP = new ResourceLocation(ComputerCraftAPI.MOD_ID, "computer");

    private final ComputerFamily family;
    protected final RegistryEntry<BlockEntityType<T>> type;
    private final BlockEntityTicker<T> serverTicker = (level, pos, state, computer) -> computer.serverTick();

    protected BlockComputerBase(Properties settings, ComputerFamily family, RegistryEntry<BlockEntityType<T>> type) {
        super(settings, type);
        this.family = family;
        this.type = type;
    }

    @Override
    @Deprecated
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);

        var tile = world.getBlockEntity(pos);
        if (tile instanceof TileComputerBase computer) computer.updateInputsImmediately();
    }

    @Override
    @Deprecated
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction incomingSide) {
        var entity = world.getBlockEntity(pos);
        if (!(entity instanceof TileComputerBase computerEntity)) return 0;

        var computer = computerEntity.getServerComputer();
        if (computer == null) return 0;

        var localSide = computerEntity.remapToLocalSide(incomingSide.getOpposite());
        return computer.getRedstoneOutput(localSide);
    }

    protected abstract ItemStack getItem(TileComputerBase tile);

    public ComputerFamily getFamily() {
        return family;
    }

    @Override
    @Deprecated
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction incomingSide) {
        return getDirectSignal(state, world, pos, incomingSide);
    }

    @Override
    public boolean getBundledRedstoneConnectivity(Level world, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        var entity = world.getBlockEntity(pos);
        if (!(entity instanceof TileComputerBase computerEntity)) return 0;

        var computer = computerEntity.getServerComputer();
        if (computer == null) return 0;

        var localSide = computerEntity.remapToLocalSide(side);
        return computer.getBundledRedstoneOutput(localSide);
    }

    @Override
    @Deprecated
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        var tile = world.getBlockEntity(pos);
        if (tile instanceof TileComputerBase computer) {
            var result = getItem(computer);
            if (!result.isEmpty()) return result;
        }

        return super.getCloneItemStack(world, pos, state);
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity tile, ItemStack tool) {
        // Don't drop blocks here - see onBlockHarvested.
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!(world instanceof ServerLevel serverWorld)) return;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        var tile = world.getBlockEntity(pos);
        if (tile instanceof TileComputerBase computer) {
            var context = new LootContext.Builder(serverWorld)
                .withRandom(world.random)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.BLOCK_ENTITY, tile)
                .withDynamicDrop(DROP, (ctx, out) -> out.accept(getItem(computer)));
            for (var item : state.getDrops(context)) {
                popResource(world, pos, item);
            }

            state.spawnAfterBreak(serverWorld, pos, player.getMainHandItem(), true);
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);

        var tile = world.getBlockEntity(pos);
        if (!world.isClientSide && tile instanceof IComputerTile computer && stack.getItem() instanceof IComputerItem item) {

            var id = item.getComputerID(stack);
            if (id != -1) computer.setComputerID(id);

            var label = item.getLabel(stack);
            if (label != null) computer.setLabel(label);
        }
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        return level.isClientSide ? null : BaseEntityBlock.createTickerHelper(type, this.type.get(), serverTicker);
    }
}
