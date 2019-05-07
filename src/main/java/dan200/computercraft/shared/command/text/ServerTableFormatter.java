/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.command.ServerCommandSource;
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
    public Component getPadding( Component component, int width )
    {
        int extraWidth = width - getWidth( component );
        if( extraWidth <= 0 ) return null;
        return new TextComponent( StringUtils.repeat( ' ', extraWidth ) );
    }

    @Override
    public int getColumnPadding()
    {
        return 1;
    }

    @Override
    public int getWidth( Component component )
    {
        return component.getText().length();
    }

    @Override
    public void writeLine( int id, Component component )
    {
        source.sendFeedback( component, false );
    }
}
