/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;

/**
 * An extension over the basic hooks, with a more convenient way of reading and writing data.
 */
public interface ContainerData {
    static <C extends ScreenHandler, T extends ContainerData> ScreenHandlerType<C> toType(Identifier identifier, Function<PacketByteBuf, T> reader,
                                                                                          Factory<C, T> factory) {
        return ScreenHandlerRegistry.registerExtended(identifier,
                                                      (id, playerInventory, packetByteBuf) -> factory.create(id,
                                                                                                             playerInventory,
                                                                                                             reader.apply(packetByteBuf)));
    }

    void toBytes(PacketByteBuf buf);

    default void open(PlayerEntity player, NamedScreenHandlerFactory owner) {
        player.openHandledScreen(owner);
    }

    interface Factory<C extends ScreenHandler, T extends ContainerData> {
        C create(int id, @Nonnull PlayerInventory inventory, T data);
    }
}
