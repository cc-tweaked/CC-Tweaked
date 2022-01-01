/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

/**
 * Provides additional data about a client computer, such as its ID and current state.
 */
public class ComputerDataClientMessage extends ComputerClientMessage
{
    private final ComputerState state;
    private final CompoundTag userData;

    public ComputerDataClientMessage( ServerComputer computer )
    {
        super( computer.getInstanceID() );
        state = computer.getState();
        userData = computer.getUserData();
    }

    public ComputerDataClientMessage( @Nonnull FriendlyByteBuf buf )
    {
        super( buf );
        state = buf.readEnum( ComputerState.class );
        userData = buf.readNbt();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeEnum( state );
        buf.writeNbt( userData );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        getComputer().setState( state, userData );
    }
}
