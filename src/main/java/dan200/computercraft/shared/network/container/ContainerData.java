/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An extension over the basic {@link IForgeContainerType}/{@link NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)}
 * hooks, with a more convenient way of reading and writing data.
 */
public interface ContainerData
{
    void toBytes( PacketBuffer buf );

    default void open( PlayerEntity player, INamedContainerProvider owner )
    {
        NetworkHooks.openGui( (ServerPlayerEntity) player, owner, this::toBytes );
    }

    static <C extends Container, T extends ContainerData> ContainerType<C> toType( Function<PacketBuffer, T> reader, Factory<C, T> factory )
    {
        return IForgeContainerType.create( ( id, player, data ) -> factory.create( id, player, reader.apply( data ) ) );
    }

    interface Factory<C extends Container, T extends ContainerData>
    {
        C create( int id, @Nonnull PlayerInventory inventory, T data );
    }
}
