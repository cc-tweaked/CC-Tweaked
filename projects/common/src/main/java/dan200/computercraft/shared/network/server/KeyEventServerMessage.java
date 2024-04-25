// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Queue a key event on the currently opened computer.
 */
public final class KeyEventServerMessage extends ComputerServerMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, KeyEventServerMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, KeyEventServerMessage::containerId,
        MoreStreamCodecs.ofEnum(Action.class), x -> x.action,
        ByteBufCodecs.INT, x -> x.key,
        KeyEventServerMessage::new
    );

    private final Action action;
    private final int key;

    public KeyEventServerMessage(AbstractContainerMenu menu, Action action, int key) {
        this(menu.containerId, action, key);
    }

    private KeyEventServerMessage(int id, Action action, int key) {
        super(id);
        this.action = action;
        this.key = key;
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        var input = container.getInput();
        switch (action) {
            case UP -> input.keyUp(key);
            case DOWN -> input.keyDown(key, false);
            case REPEAT -> input.keyDown(key, true);
        }
    }

    @Override
    public CustomPacketPayload.Type<KeyEventServerMessage> type() {
        return NetworkMessages.KEY_EVENT;
    }

    public enum Action {
        DOWN, REPEAT, UP
    }
}
