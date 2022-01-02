/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * An extension over the basic hooks, with a more convenient way of reading and writing data.
 */
public interface ContainerData
{
    void toBytes( FriendlyByteBuf buf );

    default void open( Player player, MenuProvider owner )
    {
        if( player.level.isClientSide ) return;
        player.openMenu( owner );
    }

    interface Factory<C extends AbstractContainerMenu, T extends ContainerData>
    {
        C create( int id, @Nonnull Inventory inventory, T data );
    }

    interface FixedFactory<C extends AbstractContainerMenu, T extends ContainerData>
    {
        C create( MenuType<C> type, int id, @Nonnull Inventory inventory, T data );
    }

    static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType( ResourceLocation identifier, Function<FriendlyByteBuf, T> reader,
                                                                                          Factory<C, T> factory )
    {
        return ScreenHandlerRegistry.registerExtended( identifier,
            ( id, playerInventory, packetByteBuf ) -> factory.create( id,
                playerInventory,
                reader.apply( packetByteBuf ) ) );
    }

    static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType( ResourceLocation identifier, MenuType<C> type, Function<FriendlyByteBuf, T> reader,
                                                                                          FixedFactory<C, T> factory )
    {
        return ScreenHandlerRegistry.registerExtended( identifier,
            ( id, playerInventory, packetByteBuf ) -> factory.create( type, id,
                playerInventory,
                reader.apply( packetByteBuf ) ) );
    }
}
