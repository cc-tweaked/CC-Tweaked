/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;

public interface ContainerType<T extends ScreenHandler> {
    static <C extends ScreenHandler, T extends ContainerType<C>> void register(Supplier<T> containerType, ContainerFactory<T, C> factory) {
        ContainerProviderRegistry.INSTANCE.registerFactory(containerType.get()
                                                                        .getId(), (id, type, player, packet) -> {
            T container = containerType.get();
            container.fromBytes(packet);
            return factory.apply(id, container, player);
        });
    }

    void fromBytes(PacketByteBuf buf);

    static <C extends ScreenHandler, T extends ContainerType<C>> void registerGui(Supplier<T> containerType,
                                                                                  ContainerFactory<T, HandledScreen<?>> factory) {
        ScreenProviderRegistry.INSTANCE.registerFactory(containerType.get()
                                                                     .getId(), (id, type, player, packet) -> {
            T container = containerType.get();
            container.fromBytes(packet);
            return factory.apply(id, container, player);
        });
    }

    static <C extends ScreenHandler, T extends ContainerType<C>> void registerGui(Supplier<T> containerType, BiFunction<C, PlayerInventory,
                                                                                                                           HandledScreen<?>> factory) {
        ScreenProviderRegistry.INSTANCE.<C>registerFactory(containerType.get()
                                                                        .getId(),
                                                           container -> factory.apply(container, MinecraftClient.getInstance().player.inventory));
    }

    default void open(PlayerEntity player) {
        ContainerProviderRegistry.INSTANCE.openContainer(this.getId(), player, this::toBytes);
    }

    @Nonnull
    Identifier getId();

    void toBytes(PacketByteBuf buf);

    interface ContainerFactory<T, R> {
        R apply(int id, T input, PlayerEntity player);
    }
}
