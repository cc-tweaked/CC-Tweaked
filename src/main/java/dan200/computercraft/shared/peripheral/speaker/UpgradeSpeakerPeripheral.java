/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.SpeakerStopClientMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A speaker peripheral which is used on an upgrade, and so is only attached to one computer.
 */
public abstract class UpgradeSpeakerPeripheral extends SpeakerPeripheral
{
    private final UUID source = UUID.randomUUID();

    @Override
    protected final UUID getSource()
    {
        return source;
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        // We could be in the process of shutting down the server, so we can't send packets in this case.
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get( LogicalSide.SERVER );
        if( server == null || server.isStopped() ) return;

        NetworkHandler.sendToAllPlayers( new SpeakerStopClientMessage( source ) );
    }
}
