// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.lectern;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.network.container.ContainerData;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;

/**
 * The menu for opening a {@linkplain  PocketComputerItem pocket computer} on a
 * {@linkplain CustomLecternBlockEntity lectern}.
 * <p>
 * This contains the lectern's position, so that the client is able to map the look/hit vector back to a position on the
 * computer's terminal.
 */
public final class PocketComputerLecternMenu extends ComputerMenuWithoutInventory {
    private final BlockPos lectern;

    public PocketComputerLecternMenu(int id, Inventory player, PocketHolder.LecternHolder holder, ServerComputer computer) {
        super(ModRegistry.Menus.POCKET_COMPUTER_LECTERN.get(), id, player, p -> holder.isValid(computer), computer);
        this.lectern = holder.blockPos();
    }

    public PocketComputerLecternMenu(int id, Inventory player, Data menuData) {
        super(ModRegistry.Menus.POCKET_COMPUTER_LECTERN.get(), id, player, menuData.computer());
        this.lectern = menuData.lectern();
    }

    public BlockPos lectern() {
        return lectern;
    }

    public record Data(ComputerContainerData computer, BlockPos lectern) implements ContainerData {
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
            ComputerContainerData.STREAM_CODEC, Data::computer,
            BlockPos.STREAM_CODEC, Data::lectern,
            Data::new
        );

        @Override
        public void toBytes(RegistryFriendlyByteBuf buf) {
            STREAM_CODEC.encode(buf, this);
        }
    }
}
