/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

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
        userData = buf.readCompoundTag();
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        getComputer().setState( state, userData );
    }
}
