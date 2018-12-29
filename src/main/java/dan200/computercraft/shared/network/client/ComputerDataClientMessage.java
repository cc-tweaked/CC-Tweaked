/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.core.ComputerState;
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
    private int computerId;
    private ComputerState state;
    private String label;
    private NBTTagCompound userData;

    public ComputerDataClientMessage( ServerComputer computer )
    {
        super( computer.getInstanceID() );
        this.computerId = computer.getID();
        this.state = computer.getState();
        this.label = computer.getLabel();
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

    public int getComputerId()
    {
        return computerId;
    }

    public ComputerState getState()
    {
        return state;
    }

    public String getLabel()
    {
        return label;
    }

    public NBTTagCompound getUserData()
    {
        return userData;
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeVarInt( computerId );
        buf.writeEnumValue( state );
        buf.writeBoolean( label != null );
        if( label != null ) buf.writeString( label );
        buf.writeCompoundTag( userData );
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        super.fromBytes( buf );
        computerId = buf.readVarInt();
        state = buf.readEnumValue( ComputerState.class );
        if( buf.readBoolean() ) label = buf.readString( Short.MAX_VALUE );
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
