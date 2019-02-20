/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ComputerTerminalClientMessage extends ComputerClientMessage
{
    private NBTTagCompound tag;

    public ComputerTerminalClientMessage( int instanceId, NBTTagCompound tag )
    {
        super( instanceId );
        this.tag = tag;
    }

    public ComputerTerminalClientMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeCompoundTag( tag ); // TODO: Do we need to compress this?
    }

    @Override
    public void fromBytes( @Nonnull PacketBuffer buf )
    {
        super.fromBytes( buf );
        tag = buf.readCompoundTag();
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        getComputer().readDescription( tag );
    }
}
