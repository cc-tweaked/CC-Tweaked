/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ContinueUploadMessage extends ComputerServerMessage
{
    private final boolean overwrite;

    public ContinueUploadMessage( int instanceId, boolean overwrite )
    {
        super( instanceId );
        this.overwrite = overwrite;
    }

    public ContinueUploadMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        overwrite = buf.readBoolean();
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeBoolean( overwrite );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
    {
        ServerPlayerEntity player = context.getSender();
        if( player != null ) container.confirmUpload( player, overwrite );
    }
}
