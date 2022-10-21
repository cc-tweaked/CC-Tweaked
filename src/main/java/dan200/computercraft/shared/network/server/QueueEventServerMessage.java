/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Queue an event on a {@link ServerComputer}.
 *
 * @see ServerInputHandler#queueEvent(String)
 */
public class QueueEventServerMessage extends ComputerServerMessage
{
    private final String event;
    private final Object[] args;

    public QueueEventServerMessage( Container menu, @Nonnull String event, @Nullable Object[] args )
    {
        super( menu );
        this.event = event;
        this.args = args;
    }

    public QueueEventServerMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        event = buf.readUtf( Short.MAX_VALUE );

        CompoundNBT args = buf.readNbt();
        this.args = args == null ? null : NBTUtil.decodeObjects( args );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeUtf( event );
        buf.writeNbt( args == null ? null : NBTUtil.encodeObjects( args ) );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ComputerMenu container )
    {
        container.getInput().queueEvent( event, args );
    }
}
