/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.UploadResultMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of files that have been transferred to this computer.
 *
 * @cc.module [kind=event] file_transfer.TransferredFiles
 */
public class TransferredFiles
{
    private final ServerPlayerEntity player;
    private final Container container;
    private final AtomicBoolean consumed = new AtomicBoolean( false );

    private final List<TransferredFile> files;

    public TransferredFiles( ServerPlayerEntity player, Container container, List<TransferredFile> files )
    {
        this.player = player;
        this.container = container;
        this.files = files;
    }

    /**
     * All the files that are being transferred to this computer.
     *
     * @return The list of files.
     */
    @LuaFunction
    public final List<TransferredFile> getFiles()
    {
        consumed();
        return files;
    }

    private void consumed()
    {
        if( consumed.getAndSet( true ) ) return;

        if( player.isAlive() && player.containerMenu == container )
        {
            NetworkHandler.sendToPlayer( player, UploadResultMessage.consumed( container ) );
        }
    }
}
