/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ContainerScreen;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface ContainerType<T extends Container>
{
    @Nonnull
    Identifier getId();

    void toBytes( PacketByteBuf buf );

    void fromBytes( PacketByteBuf buf );

    default void open( PlayerEntity player )
    {
        ContainerProviderRegistry.INSTANCE.openContainer( getId(), player, this::toBytes );
    }

    static <C extends Container, T extends ContainerType<C>> void register( Supplier<T> containerType, ContainerFactory<T, C> factory )
    {
        ContainerProviderRegistry.INSTANCE.registerFactory( containerType.get().getId(), ( id, type, player, packet ) -> {
            T container = containerType.get();
            container.fromBytes( packet );
            return factory.apply( id, container, player );
        } );
    }

    static <C extends Container, T extends ContainerType<C>> void registerGui( Supplier<T> containerType, ContainerFactory<T, ContainerScreen> factory )
    {
        ScreenProviderRegistry.INSTANCE.registerFactory( containerType.get().getId(), ( id, type, player, packet ) -> {
            T container = containerType.get();
            container.fromBytes( packet );
            return factory.apply( id, container, player );
        } );
    }

    static <C extends Container, T extends ContainerType<C>> void registerGui( Supplier<T> containerType, BiFunction<C, PlayerInventory, ContainerScreen> factory )
    {
        ScreenProviderRegistry.INSTANCE.<C>registerFactory( containerType.get().getId(), container ->
            factory.apply( container, MinecraftClient.getInstance().player.inventory ) );
    }

    interface ContainerFactory<T, R>
    {
        R apply( int id, T input, PlayerEntity player );
    }
}
