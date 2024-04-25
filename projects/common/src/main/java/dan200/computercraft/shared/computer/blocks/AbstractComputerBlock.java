// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.util.BlockEntityHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);

        var tile = world.getBlockEntity(pos);
        if (tile instanceof AbstractComputerBlockEntity computer) computer.updateInputsImmediately();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction incomingSide) {
        var entity = world.getBlockEntity(pos);
        if (!(entity instanceof AbstractComputerBlockEntity computerEntity)) return 0;

        var computer = computerEntity.getServerComputer();
        if (computer == null) return 0;

        var localSide = computerEntity.remapToLocalSide(incomingSide.getOpposite());
        return computer.getRedstoneOutput(localSide);
    }

    private ItemStack getItem(AbstractComputerBlockEntity tile) {
        var stack = new ItemStack(this);
        stack.applyComponents(tile.collectComponents());
        return stack;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction incomingSide) {
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
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        var tile = world.getBlockEntity(pos);
        if (tile instanceof AbstractComputerBlockEntity computer) {
            var result = getItem(computer);
            if (!result.isEmpty()) return result;
        }

        return super.getCloneItemStack(world, pos, state);
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity tile, ItemStack tool) {
        // Use the same trick as DoublePlantBlock, to skip dropping items. See playerWillDestroy.
        super.playerDestroy(world, player, pos, Blocks.AIR.defaultBlockState(), tile, tool);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        // We drop the item here instead of doing it in the harvest method, as we should
        // drop computers for creative players too.
        Block.dropResources(state, world, pos, world.getBlockEntity(pos), player, player.getMainHandItem());

        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isCrouching() && level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer) {
            // Regular right click to activate computer
            if (!level.isClientSide && computer.isUsable(player)) {
                var serverComputer = computer.createServerComputer();
                serverComputer.turnOn();

                new ComputerContainerData(serverComputer, getItem(computer)).open(player, computer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected final void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbourBlock, BlockPos neighbourPos, boolean isMoving) {
        var be = world.getBlockEntity(pos);
        if (be instanceof AbstractComputerBlockEntity computer) computer.neighborChanged(neighbourPos);
    }

    @ForgeOverride
    public final void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbour) {
        var be = world.getBlockEntity(pos);
        if (be instanceof AbstractComputerBlockEntity computer) computer.neighborChanged(neighbour);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof AbstractComputerBlockEntity computer) computer.neighbourShapeChanged(direction);

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AbstractComputerBlockEntity computer ? computer : null;
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
