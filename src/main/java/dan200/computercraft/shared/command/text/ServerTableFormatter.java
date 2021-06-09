/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
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
    @Nullable
    public Text getPadding( Text component, int width )
    {
        int extraWidth = width - this.getWidth( component );
        if( extraWidth <= 0 )
        {
            return null;
        }
        return new LiteralText( StringUtils.repeat( ' ', extraWidth ) );
    }

    @Override
    public int getColumnPadding()
    {
        return 1;
    }

    @Override
    public int getWidth( Text component )
    {
        return component.getString()
            .length();
    }

    @Override
    public void writeLine( int id, Text component )
    {
        this.source.sendFeedback( component, false );
    }
}
