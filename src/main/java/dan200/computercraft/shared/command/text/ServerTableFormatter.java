/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class ServerTableFormatter implements TableFormatter
{
    private final ServerCommandSource source;

    public ServerTableFormatter( ServerCommandSource source )
    {
        this.source = source;
    }

    @Override
    public @Nullable
    TextComponent getPadding( TextComponent component, int width )
    {
        int extraWidth = width - getWidth( component );
        if( extraWidth <= 0 ) return null;
        return new StringTextComponent( StringUtils.repeat( ' ', extraWidth ) );
    }

    @Override
    public int getColumnPadding()
    {
        return 1;
    }

    @Override
    public int getWidth( TextComponent component )
    {
        return component.getText().length();
    }

    @Override
    public void writeLine( int id, TextComponent component )
    {
        source.sendFeedback( component, false );
    }
}
