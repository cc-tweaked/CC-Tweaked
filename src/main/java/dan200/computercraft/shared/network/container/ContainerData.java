/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension over the basic {@link IForgeContainerType}/{@link NetworkHooks#openGui(ServerPlayer, MenuProvider, Consumer)}
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
        return IForgeContainerType.create( ( id, player, data ) -> factory.create( id, player, reader.apply( data ) ) );
    }

    interface Factory<C extends AbstractContainerMenu, T extends ContainerData>
    {
        C create( int id, @Nonnull Inventory inventory, T data );
    }
}
