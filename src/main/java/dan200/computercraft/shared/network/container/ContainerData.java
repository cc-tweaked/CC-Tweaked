/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension over the basic {@link IForgeMenuType}/{@link NetworkHooks#openGui(ServerPlayer, MenuProvider, Consumer)}
 * hooks, with a more convenient way of reading and writing data.
 */
public interface ContainerData
{
    void toBytes( FriendlyByteBuf buf );

    default void open( Player player, MenuProvider owner )
    {
        NetworkHooks.openGui( (ServerPlayer) player, owner, this::toBytes );
    }

    static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType( Function<FriendlyByteBuf, T> reader, Factory<C, T> factory )
    {
        return IForgeMenuType.create( ( id, player, data ) -> factory.create( id, player, reader.apply( data ) ) );
    }

    static <C extends AbstractContainerMenu, T extends ContainerData> MenuType<C> toType( Function<FriendlyByteBuf, T> reader, FixedFactory<C, T> factory )
    {
        return new FixedPointContainerFactory<>( reader, factory ).type;
    }

    interface Factory<C extends AbstractContainerMenu, T extends ContainerData>
    {
        C create( int id, @Nonnull Inventory inventory, T data );
    }

    interface FixedFactory<C extends AbstractContainerMenu, T extends ContainerData>
    {
        C create( MenuType<C> type, int id, @Nonnull Inventory inventory, T data );
    }

    class FixedPointContainerFactory<C extends AbstractContainerMenu, T extends ContainerData> implements IContainerFactory<C>
    {
        private final IContainerFactory<C> impl;
        private final MenuType<C> type;

        private FixedPointContainerFactory( Function<FriendlyByteBuf, T> reader, FixedFactory<C, T> factory )
        {
            MenuType<C> type = this.type = IForgeMenuType.create( this );
            impl = ( id, player, data ) -> factory.create( type, id, player, reader.apply( data ) );
        }

        @Override
        public C create( int windowId, Inventory inv, FriendlyByteBuf data )
        {
            return impl.create( windowId, inv, data );
        }
    }
}
