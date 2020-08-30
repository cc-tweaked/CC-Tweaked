/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension over the basic
 * hooks, with a more convenient way of reading and writing data.
 */
public interface ContainerData
{
    void toBytes( PacketByteBuf buf );

    //TODO Figure out what the heck to do here
//    default void open( PlayerEntity player, NamedScreenHandlerFactory owner )
//    {
//        NetworkHooks.openGui( (ServerPlayerEntity) player, owner, this::toBytes );
//    }
//
//    static <C extends ScreenHandler, T extends ContainerData> ScreenHandlerType<C> toType( Function<PacketByteBuf, T> reader, Factory<C, T> factory )
//    {
//        return IForgeContainerType.create( ( id, player, data ) -> factory.create( id, player, reader.apply( data ) ) );
//    }
//
//    interface Factory<C extends ScreenHandler, T extends ContainerData>
//    {
//        C create( int id, @Nonnull PlayerInventory inventory, T data );
//    }
}
