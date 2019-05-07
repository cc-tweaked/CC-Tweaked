/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.ChatFormat;
import net.minecraft.network.chat.*;
import net.minecraft.util.math.BlockPos;

/**
 * Various helpers for building chat messages
 */
public final class ChatHelpers
{
    private static final ChatFormat HEADER = ChatFormat.LIGHT_PURPLE;

    private ChatHelpers() {}

    public static Component coloured( String text, ChatFormat colour )
    {
        TextComponent component = new TextComponent( text == null ? "" : text );
        component.getStyle().setColor( colour );
        return component;
    }

    public static <T extends Component> T coloured( T component, ChatFormat colour )
    {
        component.getStyle().setColor( colour );
        return component;
    }

    public static Component text( String text )
    {
        return new TextComponent( text == null ? "" : text );
    }

    public static Component translate( String text )
    {
        return new TranslatableComponent( text == null ? "" : text );
    }

    public static Component translate( String text, Object... args )
    {
        return new TranslatableComponent( text == null ? "" : text, args );
    }

    public static Component list( Component... children )
    {
        Component component = new TextComponent( "" );
        for( Component child : children )
        {
            component.append( child );
        }
        return component;
    }

    public static Component position( BlockPos pos )
    {
        if( pos == null ) return translate( "commands.computercraft.generic.no_position" );
        return translate( "commands.computercraft.generic.position", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static Component bool( boolean value )
    {
        return value
            ? coloured( translate( "commands.computercraft.generic.yes" ), ChatFormat.GREEN )
            : coloured( translate( "commands.computercraft.generic.no" ), ChatFormat.RED );
    }

    public static Component link( Component component, String command, Component toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style.setColor( ChatFormat.YELLOW );
        style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, toolTip ) );

        return component;
    }

    public static Component header( String text )
    {
        return coloured( text, HEADER );
    }
}
