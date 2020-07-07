/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface ContainerType<T extends ScreenHandler>
{
    @Nonnull
    Identifier getId();

    void toBytes( PacketByteBuf buf );

    void fromBytes( PacketByteBuf buf );

    default void open( PlayerEntity player )
    {
        ContainerProviderRegistry.INSTANCE.openContainer( getId(), player, this::toBytes );
    }

    static <C extends ScreenHandler, T extends ContainerType<C>> void register( Supplier<T> containerType, ContainerFactory<T, C> factory )
    {
        ContainerProviderRegistry.INSTANCE.registerFactory( containerType.get().getId(), ( id, type, player, packet ) -> {
            T container = containerType.get();
            container.fromBytes( packet );
            return factory.apply( id, container, player );
        } );
    }

    static <C extends ScreenHandler, T extends ContainerType<C>> void registerGui( Supplier<T> containerType, ContainerFactory<T, HandledScreen<?>> factory )
    {
        ScreenProviderRegistry.INSTANCE.registerFactory( containerType.get().getId(), ( id, type, player, packet ) -> {
            T container = containerType.get();
            container.fromBytes( packet );
            return factory.apply( id, container, player );
        } );
    }

    static <C extends ScreenHandler, T extends ContainerType<C>> void registerGui( Supplier<T> containerType, BiFunction<C, PlayerInventory, HandledScreen<?>> factory )
    {
        ScreenProviderRegistry.INSTANCE.<C>registerFactory( containerType.get().getId(), container ->
            factory.apply( container, MinecraftClient.getInstance().player.inventory ) );
    }

    interface ContainerFactory<T, R>
    {
        R apply( int id, T input, PlayerEntity player );
    }
}
