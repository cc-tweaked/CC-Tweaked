/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.network.PacketByteBuf;

import javax.annotation.Nonnull;

public class ComputerActionServerMessage extends ComputerServerMessage
{
    private final Action action;

    public ComputerActionServerMessage( int instanceId, Action action )
    {
        super( instanceId );
        this.action = action;
    }

    public ComputerActionServerMessage( @Nonnull PacketByteBuf buf )
    {
        super( buf );
        action = buf.readEnumConstant( Action.class );
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        super.toBytes( buf );
        buf.writeEnumConstant( action );
    }

    @Override
    protected void handle( PacketContext context, @Nonnull ServerComputer computer, @Nonnull IContainerComputer container )
    {
        switch( action )
        {
            case TURN_ON:
                computer.turnOn();
                break;
            case REBOOT:
                computer.reboot();
                break;
            case SHUTDOWN:
                computer.shutdown();
                break;
        }
    }

    public enum Action
    {
        TURN_ON, SHUTDOWN, REBOOT
    }
}
