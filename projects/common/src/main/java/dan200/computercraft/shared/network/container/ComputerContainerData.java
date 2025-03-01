// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The data required to open a computer container.
 *
 * @param family        The computer family.
 * @param terminal      The initial terminal contents.
 * @param displayStack  The stack associated with this menu. This may be displayed on the client.
 * @param uploadMaxSize The maximum size of a file upload.
 */
public record ComputerContainerData(
    ComputerFamily family, TerminalState terminal, ItemStack displayStack, int uploadMaxSize
) implements ContainerData {
    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerContainerData> STREAM_CODEC = StreamCodec.composite(
        MoreStreamCodecs.ofEnum(ComputerFamily.class), ComputerContainerData::family,
        TerminalState.STREAM_CODEC, ComputerContainerData::terminal,
        ItemStack.OPTIONAL_STREAM_CODEC, ComputerContainerData::displayStack,
        ByteBufCodecs.VAR_INT, ComputerContainerData::uploadMaxSize,
        ComputerContainerData::new
    );

    public ComputerContainerData(ServerComputer computer, ItemStack displayStack) {
        this(computer.getFamily(), computer.getTerminalState(), displayStack, Config.uploadMaxSize);
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        STREAM_CODEC.encode(buf, this);
    }
}
