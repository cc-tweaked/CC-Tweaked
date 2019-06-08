/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * A horrible hack to allow opening GUIs until Forge adds a built-in system.
 */
public interface ContainerData<T extends Container> extends INamedContainerProvider
{
    void toBytes( PacketBuffer buf );

    default void open( PlayerEntity player )
    {
        NetworkHooks.openGui( (ServerPlayerEntity) player, this, this::toBytes );
    }

    @Nonnull
    T createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player );

    static <C extends Container, T extends ContainerData<C>> net.minecraft.inventory.container.ContainerType<C> create( Function<PacketBuffer, T> reader )
    {
        return new net.minecraft.inventory.container.ContainerType<>(
            ( id, player ) -> reader.apply( null ).createMenu( id, player, player.player )
        );
    }
}
