/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.text;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class ServerTableFormatter implements TableFormatter
{
    private final ICommandSender source;

    public ServerTableFormatter( ICommandSender source )
    {
        this.source = source;
    }

    @Override
    @Nullable
    public ITextComponent getPadding( ITextComponent component, int width )
    {
        int extraWidth = width - getWidth( component );
        if( extraWidth <= 0 ) return null;
        return new TextComponentString( StringUtils.repeat( ' ', extraWidth ) );
    }

    @Override
    public int getColumnPadding()
    {
        return 1;
    }

    @Override
    public int getWidth( ITextComponent component )
    {
        return component.getUnformattedText().length();
    }

    @Override
    public void writeLine( int id, ITextComponent component )
    {
        source.sendMessage( component );
    }
}
