/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Function;

/**
 * Additional data to send when opening a menu. Like {@link NetworkMessage}, this should be immutable.
 */
public interface ContainerData {
    void toBytes(FriendlyByteBuf buf);

    /**
     * Open a menu for a specific player using this data.
     *
     * @param player The player to open the menu for.
     * @param menu   The underlying menu provider.
     */
    default void open(Player player, MenuProvider menu) {
        PlatformHelper.get().openMenu(player, menu, this);
    }

    static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType(Function<FriendlyByteBuf, T> reader, Factory<C, T> factory) {
        return PlatformHelper.get().createMenuType(reader, factory);
    }

    interface Factory<C extends AbstractContainerMenu, T extends ContainerData> {
        C create(int id, Inventory inventory, T data);
    }
}
