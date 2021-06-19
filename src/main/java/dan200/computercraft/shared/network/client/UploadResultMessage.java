/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.gui.ComputerScreenBase;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

public class UploadResultMessage implements NetworkMessage
{
    public static final UploadResultMessage COMPUTER_OFF = new UploadResultMessage( UploadResult.ERROR, UploadResult.COMPUTER_OFF_MSG );
    public static final UploadResultMessage OUT_OF_SPACE = new UploadResultMessage( UploadResult.ERROR, UploadResult.OUT_OF_SPACE_MSG );

    private final UploadResult result;
    private final ITextComponent message;

    public UploadResultMessage( UploadResult result, ITextComponent message )
    {
        this.result = result;
        this.message = message;
    }

    public UploadResultMessage( @Nonnull PacketBuffer buf )
    {
        result = buf.readEnum( UploadResult.class );
        message = buf.readComponent();
    }

    @Override
    public void toBytes( @Nonnull PacketBuffer buf )
    {
        buf.writeEnum( result );
        buf.writeComponent( message );
    }

    @Override
    public void handle( NetworkEvent.Context context )
    {
        Minecraft minecraft = Minecraft.getInstance();

        if( minecraft.screen instanceof ComputerScreenBase<?> )
        {
            ((ComputerScreenBase<?>) minecraft.screen).uploadResult( result, message );
        }
    }
}
