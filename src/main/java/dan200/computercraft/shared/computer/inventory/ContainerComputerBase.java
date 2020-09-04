/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.network.container.ComputerContainerData;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class ContainerComputerBase extends ScreenHandler implements IContainerComputer {
    private final Predicate<PlayerEntity> canUse;
    private final IComputer computer;
    private final ComputerFamily family;
    private final InputState input = new InputState(this);

    protected ContainerComputerBase(ScreenHandlerType<? extends ContainerComputerBase> type, int id, PlayerInventory player, PacketByteBuf packetByteBuf) {
        this(type,
             id,
             x -> true,
             getComputer(player, new ComputerContainerData(new PacketByteBuf(packetByteBuf.copy()))),
             new ComputerContainerData(packetByteBuf).getFamily());
    }

    protected ContainerComputerBase(ScreenHandlerType<? extends ContainerComputerBase> type, int id, Predicate<PlayerEntity> canUse, IComputer computer,
                                    ComputerFamily family) {
        super(type, id);
        this.canUse = canUse;
        this.computer = Objects.requireNonNull(computer);
        this.family = family;
    }

    protected static IComputer getComputer(PlayerInventory player, ComputerContainerData data) {
        int id = data.getInstanceId();
        if (!player.player.world.isClient) {
            return ComputerCraft.serverComputerRegistry.get(id);
        }

        ClientComputer computer = ComputerCraft.clientComputerRegistry.get(id);
        if (computer == null) {
            ComputerCraft.clientComputerRegistry.add(id, computer = new ClientComputer(id));
        }
        return computer;
    }

    @Nonnull
    public ComputerFamily getFamily() {
        return this.family;
    }

    @Nullable
    @Override
    public IComputer getComputer() {
        return this.computer;
    }

    @Nonnull
    @Override
    public InputState getInput() {
        return this.input;
    }

    @Override
    public void close(@Nonnull PlayerEntity player) {
        super.close(player);
        this.input.close();
    }

    @Override
    public boolean canUse(@Nonnull PlayerEntity player) {
        return this.canUse.test(player);
    }
}
