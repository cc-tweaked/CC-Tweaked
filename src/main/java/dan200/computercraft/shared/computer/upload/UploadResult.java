/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum UploadResult
{
    QUEUED,
    CONSUMED,
    ERROR;

    public static final ITextComponent SUCCESS_TITLE = new TranslationTextComponent( "gui.computercraft.upload.success" );

    public static final ITextComponent FAILED_TITLE = new TranslationTextComponent( "gui.computercraft.upload.failed" );
    public static final ITextComponent COMPUTER_OFF_MSG = new TranslationTextComponent( "gui.computercraft.upload.failed.computer_off" );
    public static final ITextComponent TOO_MUCH_MSG = new TranslationTextComponent( "gui.computercraft.upload.failed.too_much" );
}
