// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public abstract class AbstractComputerBlock<T extends AbstractComputerBlockEntity> extends HorizontalDirectionalBlock implements IBundledRedstoneBlock, EntityBlock {
    private static final ResourceLocation DROP = new ResourceLocation(ComputerCraftAPI.MOD_ID, "computer");

    protected final RegistryEntry<BlockEntityType<T>> type;
    private final BlockEntityTicker<T> serverTicker = (level, pos, state, computer) -> computer.serverTick();

    protected AbstractComputerBlock(Properties settings, RegistryEntry<BlockEntityType<T>> type) {
        super(settings);
        this.type = type;
    }

    @Override
    @Deprecated
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);

        var tile = world.getBlockEntity(pos);
        if (tile instanceof AbstractComputerBlockEntity computer) computer.updateInputsImmediately();
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
        if (!(entity instanceof AbstractComputerBlockEntity computerEntity)) return 0;

        var computer = computerEntity.getServerComputer();
        if (computer == null) return 0;

        var localSide = computerEntity.remapToLocalSide(incomingSide.getOpposite());
        return computer.getRedstoneOutput(localSide);
    }

    protected abstract ItemStack getItem(AbstractComputerBlockEntity tile);

    @Override
    @Deprecated
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction incomingSide) {
        return getDirectSignal(state, world, pos, incomingSide);
    }

    @Override
    public int getBundledRedstoneOutput(Level world, BlockPos pos, Direction side) {
        var entity = world.getBlockEntity(pos);
        if (!(entity instanceof AbstractComputerBlockEntity computerEntity)) return 0;

        var computer = computerEntity.getServerComputer();
        if (computer == null) return 0;

        var localSide = computerEntity.remapToLocalSide(side);
        return computer.getBundledRedstoneOutput(localSide);
    }

    @Override
    @Deprecated
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        var tile = world.getBlockEntity(pos);
        if (tile instanceof AbstractComputerBlockEntity computer) {
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
        super.playerWillDestroy(world, pos, state, player);
        if (!(world instanceof ServerLevel serverWorld)) return;

        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.

        var tile = world.getBlockEntity(pos);
        if (tile instanceof AbstractComputerBlockEntity computer) {
            var context = new LootParams.Builder(serverWorld)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.BLOCK_ENTITY, tile)
                .withDynamicDrop(DROP, out -> out.accept(getItem(computer)));
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
        if (!world.isClientSide && tile instanceof AbstractComputerBlockEntity computer && stack.getItem() instanceof IComputerItem item) {

            var id = item.getComputerID(stack);
            if (id != -1) computer.setComputerID(id);

            var label = item.getLabel(stack);
            if (label != null) computer.setLabel(label);
        }
    }

    @Override
    @Deprecated
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isCrouching() && level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer) {
            // Regular right click to activate computer
            if (!level.isClientSide && computer.isUsable(player)) {
                var serverComputer = computer.createServerComputer();
                serverComputer.turnOn();

                new ComputerContainerData(serverComputer, getItem(computer)).open(player, computer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    @Deprecated
    public final void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean isMoving) {
        var be = world.getBlockEntity(pos);
        if (be instanceof AbstractComputerBlockEntity computer) computer.neighborChanged(neighbourPos);
    }

    @ForgeOverride
    public final void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbour) {
        var be = world.getBlockEntity(pos);
        if (be instanceof AbstractComputerBlockEntity computer) computer.neighborChanged(neighbour);
    }

    @Override
    @Deprecated
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof AbstractComputerBlockEntity computer) computer.neighbourShapeChanged(direction);

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        return level.isClientSide ? null : BlockEntityHelpers.createTickerHelper(type, this.type.get(), serverTicker);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }
}
