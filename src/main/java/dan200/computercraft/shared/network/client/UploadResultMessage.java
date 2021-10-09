/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.gui.ComputerScreenBase;
import dan200.computercraft.client.gui.OptionScreen;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkMessage;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import javax.annotation.Nonnull;

public class UploadResultMessage implements NetworkMessage
{
    public static final UploadResultMessage COMPUTER_OFF = new UploadResultMessage( UploadResult.ERROR, UploadResult.COMPUTER_OFF_MSG );
    public static final UploadResultMessage OUT_OF_SPACE = new UploadResultMessage( UploadResult.ERROR, UploadResult.OUT_OF_SPACE_MSG );

    private final UploadResult result;
    private final Text message;

    public UploadResultMessage( UploadResult result, Text message )
    {
        this.result = result;
        this.message = message;
    }

    public UploadResultMessage( @Nonnull PacketByteBuf buf )
    {
        result = buf.readEnumConstant( UploadResult.class );
        message = buf.readText();
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeEnumConstant( result );
        buf.writeText( message );
    }

    @Override
    public void handle( PacketContext context )
    {
        MinecraftClient minecraft = MinecraftClient.getInstance();

        Screen screen = OptionScreen.unwrap( minecraft.currentScreen );
        if( screen instanceof ComputerScreenBase<?> )
        {
            ((ComputerScreenBase<?>) screen).uploadResult( result, message );
        }
    }
}
