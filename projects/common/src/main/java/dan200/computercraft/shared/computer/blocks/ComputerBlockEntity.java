// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.TerminalSize;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ComputerBlockEntity extends AbstractComputerBlockEntity {
    private static final String NBT_TERMINAL_SIZE = "TerminalSize";

    private @Nullable TerminalSize terminalSize;

    private @Nullable IPeripheral peripheral;

    public ComputerBlockEntity(BlockEntityType<? extends ComputerBlockEntity> type, BlockPos pos, BlockState state, ComputerFamily family) {
        super(type, pos, state, family);
    }

    @Override
    protected void loadServer(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadServer(nbt, registries);
        terminalSize = NBTUtil.decodeFrom(TerminalSize.CODEC, registries, nbt, NBT_TERMINAL_SIZE);
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        NBTUtil.encodeTo(TerminalSize.CODEC, registries, nbt, NBT_TERMINAL_SIZE, terminalSize);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter component) {
        super.applyImplicitComponents(component);
        terminalSize = component.get(ModRegistry.DataComponents.TERMINAL_SIZE.get());
    }

    @Override
    protected void collectSafeComponents(DataComponentMap.Builder builder) {
        super.collectSafeComponents(builder);
        builder.set(ModRegistry.DataComponents.TERMINAL_SIZE.get(), terminalSize);
    }

    @Override
    @Deprecated
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(NBT_TERMINAL_SIZE);
    }

    @Override
    protected ServerComputer createComputer(int id) {
        return new ServerComputer((ServerLevel) getLevel(), getBlockPos(), ServerComputer.properties(id, getFamily())
            .label(getLabel())
            .terminalSize(terminalSize != null ? terminalSize : new TerminalSize(ConfigSpec.computerTermWidth.get(), ConfigSpec.computerTermHeight.get()))
            .storageCapacity(storageCapacity)
        );
    }

    protected boolean isUsableByPlayer(Player player) {
        return isUsable(player);
    }

    @Override
    public Direction getDirection() {
        return getBlockState().getValue(ComputerBlock.FACING);
    }

    @Override
    protected void updateBlockState(ComputerState newState) {
        var existing = getBlockState();
        if (existing.getValue(ComputerBlock.STATE) != newState) {
            getLevel().setBlock(getBlockPos(), existing.setValue(ComputerBlock.STATE, newState), ComputerBlock.UPDATE_CLIENTS);
        }
    }

    @Override
    protected ComputerSide remapLocalSide(ComputerSide localSide) {
        // For legacy reasons, computers invert the meaning of "left" and "right". A computer's front is facing
        // towards you, but a turtle's front is facing the other way.
        if (localSide == ComputerSide.RIGHT) return ComputerSide.LEFT;
        if (localSide == ComputerSide.LEFT) return ComputerSide.RIGHT;
        return localSide;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ComputerMenuWithoutInventory(ModRegistry.Menus.COMPUTER.get(), id, inventory, this::isUsableByPlayer, createServerComputer());
    }

    public IPeripheral peripheral() {
        if (peripheral != null) return peripheral;
        return peripheral = new ComputerPeripheral("computer", this);
    }
}
