/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.server;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ComputerActionServerMessage extends ComputerServerMessage
{
    private final Action action;

    public ComputerActionServerMessage( Container menu, Action action )
    {
        super( menu );
        this.action = action;
    }

    public ComputerActionServerMessage( @Nonnull PacketBuffer buf )
    {
        super( buf );
        action = buf.readEnum( Action.class );
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        super.toBytes( buf );
        buf.writeEnum( action );
    }

    @Override
    protected void handle( NetworkEvent.Context context, @Nonnull ComputerMenu container )
    {
        switch( action )
        {
            case TURN_ON:
                container.getInput().turnOn();
                break;
            case REBOOT:
                container.getInput().reboot();
                break;
            case SHUTDOWN:
                container.getInput().shutdown();
                break;
        }
    }

    public enum Action
    {
        TURN_ON,
        SHUTDOWN,
        REBOOT
    }
}
