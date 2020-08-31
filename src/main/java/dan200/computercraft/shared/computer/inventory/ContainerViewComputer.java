/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.inventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.InputState;
import dan200.computercraft.shared.computer.core.ServerComputer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;

public class ContainerViewComputer extends ScreenHandler implements IContainerComputer {
    private final IComputer computer;
    private final InputState input = new InputState(this);

    public ContainerViewComputer(int id, IComputer computer) {
        super(null, id);
        this.computer = computer;
    }

    private static boolean canInteractWith(@Nonnull ServerComputer computer, @Nonnull PlayerEntity player) {
        // If this computer no longer exists then discard it.
        if (ComputerCraft.serverComputerRegistry.get(computer.getInstanceID()) != computer) {
            return false;
        }

        // If we're a command computer then ensure we're in creative
        return computer.getFamily() != ComputerFamily.COMMAND || TileCommandComputer.isUsable(player);
    }

    @Nullable
    @Override
    public IComputer getComputer() {
        return this.computer;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.computer instanceof ServerComputer) {
            ServerComputer serverComputer = (ServerComputer) this.computer;

            // If this computer no longer exists then discard it.
            if (ComputerCraft.serverComputerRegistry.get(serverComputer.getInstanceID()) != serverComputer) {
                return false;
            }

            // If we're a command computer then ensure we're in creative
            if (serverComputer.getFamily() == ComputerFamily.COMMAND) {
                MinecraftServer server = player.getServer();
                if (server == null || !server.areCommandBlocksEnabled()) {
                    player.sendMessage(new TranslatableText("advMode.notEnabled"), false);
                    return false;
                } else if (!player.isCreativeLevelTwoOp()) {
                    player.sendMessage(new TranslatableText("advMode.notAllowed"), false);
                    return false;
                }
            }
        }

        return true;
    }

    @Nonnull
    @Override
    public InputState getInput() {
        return this.input;
    }

    @Override
    public void close(PlayerEntity player) {
        super.close(player);
        this.input.close();
    }
}
