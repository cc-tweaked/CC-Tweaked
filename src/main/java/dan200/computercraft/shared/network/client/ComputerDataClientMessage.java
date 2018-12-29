/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.blocks.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Provides additional data about a client computer, such as its ID and current state.
 */
public class ComputerDataClientMessage extends ComputerClientMessage
{
    private ComputerState state;
    private NBTTagCompound userData;

    public ComputerDataClientMessage( ServerComputer computer )
    {
        super( computer.getInstanceID() );
        this.state = computer.getState();
        this.userData = computer.getUserData();
    }

    public ComputerDataClientMessage()
    {
    }

    @Override
    public int getId()
    {
        return NetworkMessages.COMPUTER_DATA_CLIENT_MESSAGE;
    }

    public ComputerState getState()
    {
        return state;
    }

    public NBTTagCompound getUserData()
    {
        return userData;
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeEnumValue( state );
        buf.writeCompoundTag( userData );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        super.fromBytes( buf );
        state = buf.readEnumValue( ComputerState.class );
        try
        {
            userData = buf.readCompoundTag();
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }
}
