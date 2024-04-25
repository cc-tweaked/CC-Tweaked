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
 * Queue a mouse event on the currently opened computer.
 */
public final class MouseEventServerMessage extends ComputerServerMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, MouseEventServerMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MouseEventServerMessage::containerId,
        MoreStreamCodecs.ofEnum(Action.class), x -> x.action,
        ByteBufCodecs.VAR_INT, x -> x.arg,
        ByteBufCodecs.VAR_INT, x -> x.x,
        ByteBufCodecs.VAR_INT, x -> x.y,
        MouseEventServerMessage::new
    );

    private final Action action;
    private final int arg;
    private final int x;
    private final int y;

    public MouseEventServerMessage(AbstractContainerMenu menu, Action action, int arg, int x, int y) {
        this(menu.containerId, action, arg, x, y);
    }

    private MouseEventServerMessage(int id, Action action, int arg, int x, int y) {
        super(id);
        this.action = action;
        this.x = x;
        this.y = y;
        this.arg = arg;
    }

    @Override
    protected void handle(ServerNetworkContext context, ComputerMenu container) {
        var input = container.getInput();
        switch (action) {
            case CLICK -> input.mouseClick(arg, x, y);
            case DRAG -> input.mouseDrag(arg, x, y);
            case UP -> input.mouseUp(arg, x, y);
            case SCROLL -> input.mouseScroll(arg, x, y);
        }
    }

    @Override
    public CustomPacketPayload.Type<MouseEventServerMessage> type() {
        return NetworkMessages.MOUSE_EVENT;
    }

    public enum Action {
        CLICK, DRAG, UP, SCROLL,
    }
}
