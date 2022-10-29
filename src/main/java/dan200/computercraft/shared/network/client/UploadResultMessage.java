/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.gui.ComputerScreenBase;
import dan200.computercraft.client.gui.OptionScreen;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UploadResultMessage implements NetworkMessage
{
    private final int containerId;
    private final UploadResult result;
    private final ITextComponent errorMessage;

    private UploadResultMessage( Container container, UploadResult result, @Nullable ITextComponent errorMessage )
    {
        containerId = container.containerId;
        this.result = result;
        this.errorMessage = errorMessage;
    }

    public static UploadResultMessage queued( Container container )
    {
        return new UploadResultMessage( container, UploadResult.QUEUED, null );
    }

    public static UploadResultMessage consumed( Container container )
    {
        return new UploadResultMessage( container, UploadResult.CONSUMED, null );
    }

    public static UploadResultMessage error( Container container, ITextComponent errorMessage )
    {
        return new UploadResultMessage( container, UploadResult.ERROR, errorMessage );
    }

    public UploadResultMessage( @Nonnull PacketBuffer buf )
    {
        containerId = buf.readVarInt();
        result = buf.readEnum( UploadResult.class );
        errorMessage = result == UploadResult.ERROR ? buf.readComponent() : null;
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeVarInt( containerId );
        buf.writeEnum( result );
        if( result == UploadResult.ERROR ) buf.writeComponent( errorMessage );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        Minecraft minecraft = Minecraft.getInstance();

        Screen screen = OptionScreen.unwrap( minecraft.screen );
        if( screen instanceof ComputerScreenBase<?> && ((ComputerScreenBase<?>) screen).getMenu().containerId == containerId )
        {
            ((ComputerScreenBase<?>) screen).uploadResult( result, errorMessage );
        }
    }
}
