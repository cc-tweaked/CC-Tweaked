/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ComputerActionServerMessage extends ComputerServerMessage {
    private final Action action;

    public ComputerActionServerMessage(AbstractContainerMenu menu, Action action) {
        super(menu);
        this.action = action;
    }

    public ComputerActionServerMessage(@Nonnull FriendlyByteBuf buf) {
        super(buf);
        action = buf.readEnum(Action.class);
    }

    @Override
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeEnum(action);
    }

    @Override
    protected void handle(NetworkEvent.Context context, @Nonnull ComputerMenu container) {
        switch (action) {
            case TURN_ON -> container.getInput().turnOn();
            case REBOOT -> container.getInput().reboot();
            case SHUTDOWN -> container.getInput().shutdown();
        }
    }

    public enum Action {
        TURN_ON,
        SHUTDOWN,
        REBOOT
    }
}
