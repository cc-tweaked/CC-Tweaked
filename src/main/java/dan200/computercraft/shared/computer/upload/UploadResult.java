/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public enum UploadResult
{
    SUCCESS,
    ERROR,
    CONFIRM_OVERWRITE;

    public static final Text SUCCESS_TITLE = new TranslatableText( "gui.computercraft.upload.success" );

    public static final Text FAILED_TITLE = new TranslatableText( "gui.computercraft.upload.failed" );
    public static final Text COMPUTER_OFF_MSG = new TranslatableText( "gui.computercraft.upload.failed.computer_off" );
    public static final Text OUT_OF_SPACE_MSG = new TranslatableText( "gui.computercraft.upload.failed.out_of_space" );
    public static final Text TOO_MUCH_MSG = new TranslatableText( "gui.computercraft.upload.failed.too_much" );

    public static final Text UPLOAD_OVERWRITE = new TranslatableText( "gui.computercraft.upload.overwrite" );
}
