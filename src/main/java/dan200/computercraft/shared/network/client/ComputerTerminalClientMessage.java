/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;

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
    public int getId()
    {
        return NetworkMessages.COMPUTER_TERMINAL_CLIENT_MESSAGE;
    }

    public NBTTagCompound getTag()
    {
        return tag;
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
        try
        {
            tag = buf.readCompoundTag();
        }
        catch( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }
}
