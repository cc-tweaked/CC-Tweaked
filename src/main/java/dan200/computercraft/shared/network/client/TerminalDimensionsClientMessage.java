/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.NetworkMessage;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.network.PacketByteBuf;

import javax.annotation.Nonnull;

/**
 * The terminal and portable computer server-side configured dimensions.
 */
public class TerminalDimensionsClientMessage implements NetworkMessage
{

    private final int computerTermWidth;
    private final int computerTermHeight;
    private final int pocketTermWidth;
    private final int pocketTermHeight;
    private final int monitorWidth;
    private final int monitorHeight;

    public TerminalDimensionsClientMessage()
    {
        this.computerTermWidth = ComputerCraft.computerTermWidth;
        this.computerTermHeight = ComputerCraft.computerTermHeight;
        this.pocketTermWidth = ComputerCraft.pocketTermWidth;
        this.pocketTermHeight = ComputerCraft.pocketTermHeight;
        this.monitorHeight = ComputerCraft.monitorHeight;
        this.monitorWidth = ComputerCraft.monitorWidth;
    }

    public TerminalDimensionsClientMessage( @Nonnull PacketByteBuf buf )
    {
        computerTermWidth = buf.readVarInt();
        computerTermHeight = buf.readVarInt();
        pocketTermWidth = buf.readVarInt();
        pocketTermHeight = buf.readVarInt();
        monitorHeight = buf.readVarInt();
        monitorWidth = buf.readVarInt();
    }

    @Override
    public void toBytes( PacketByteBuf buf )
    {
        buf.writeVarInt( computerTermWidth );
        buf.writeVarInt( computerTermHeight );
        buf.writeVarInt( pocketTermWidth );
        buf.writeVarInt( pocketTermHeight );
        buf.writeVarInt( monitorWidth );
        buf.writeVarInt( monitorHeight );
    }

    @Override
    public void handle( PacketContext context )
    {
        ComputerCraft.computerTermWidth = this.computerTermWidth;
        ComputerCraft.computerTermHeight = this.computerTermHeight;
        ComputerCraft.pocketTermWidth = this.pocketTermWidth;
        ComputerCraft.pocketTermHeight = this.pocketTermHeight;
        ComputerCraft.monitorWidth = this.monitorWidth;
        ComputerCraft.monitorHeight = this.monitorHeight;
    }

}
