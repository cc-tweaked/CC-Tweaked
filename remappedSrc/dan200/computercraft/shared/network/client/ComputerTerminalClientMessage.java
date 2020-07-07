/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.client;

import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import javax.annotation.Nonnull;

public class ComputerTerminalClientMessage extends ComputerClientMessage
{
    private CompoundTag tag;

    public ComputerTerminalClientMessage( int instanceId, CompoundTag tag )
    {
        super( instanceId );
        this.tag = tag;
    }

    public ComputerTerminalClientMessage()
    {
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeCompoundTag( tag ); // TODO: Do we need to compress this?
    }

    @Override
    public void fromBytes( @Nonnull PacketByteBuf buf )
    {
        super.fromBytes( buf );
        tag = buf.readCompoundTag();
    }

    @Override
    public void handle( PacketContext context )
    {
        getComputer().readDescription( tag );
    }
}
