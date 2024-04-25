// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Additional data to send when opening a menu. Like {@link NetworkMessage}, this should be immutable.
 */
public interface ContainerData {
    void toBytes(RegistryFriendlyByteBuf buf);

    /**
     * Open a menu for a specific player using this data.
     *
     * @param player The player to open the menu for.
     * @param menu   The underlying menu provider.
     */
    default void open(Player player, MenuProvider menu) {
        PlatformHelper.get().openMenu(player, menu, this);
    }

    static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType(StreamCodec<RegistryFriendlyByteBuf, T> codec, Factory<C, T> factory) {
        return PlatformHelper.get().createMenuType(codec, factory);
    }

    interface Factory<C extends AbstractContainerMenu, T extends ContainerData> {
        C create(int id, Inventory inventory, T data);
    }
}
