// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.OverridingMethodsMustInvokeSuper;

/**
 * A packet, which performs an action on the currently open {@link ComputerMenu}.
 */
public abstract class ComputerServerMessage implements NetworkMessage<ServerNetworkContext> {
    private final int containerId;

    protected ComputerServerMessage(AbstractContainerMenu menu) {
        containerId = menu.containerId;
    }

    public ComputerServerMessage(FriendlyByteBuf buffer) {
        containerId = buffer.readVarInt();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
    }

    @Override
    public void handle(ServerNetworkContext context) {
        Player player = context.getSender();
        if (player.containerMenu.containerId == containerId && player.containerMenu instanceof ComputerMenu) {
            handle(context, (ComputerMenu) player.containerMenu);
        }
    }

    protected abstract void handle(ServerNetworkContext context, ComputerMenu container);
}
