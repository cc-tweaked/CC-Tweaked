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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class UploadResultMessage implements NetworkMessage
{
    public static final UploadResultMessage COMPUTER_OFF = new UploadResultMessage( UploadResult.ERROR, UploadResult.COMPUTER_OFF_MSG );
    public static final UploadResultMessage OUT_OF_SPACE = new UploadResultMessage( UploadResult.ERROR, UploadResult.OUT_OF_SPACE_MSG );

    private final UploadResult result;
    private final Component message;

    public UploadResultMessage( UploadResult result, Component message )
    {
        this.result = result;
        this.message = message;
    }

    public UploadResultMessage( @Nonnull FriendlyByteBuf buf )
    {
        result = buf.readEnum( UploadResult.class );
        message = buf.readComponent();
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeEnum( result );
        buf.writeComponent( message );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        Minecraft minecraft = Minecraft.getInstance();

        Screen screen = OptionScreen.unwrap( minecraft.screen );
        if( screen instanceof ComputerScreenBase<?> )
        {
            ((ComputerScreenBase<?>) screen).uploadResult( result, message );
        }
    }
}
