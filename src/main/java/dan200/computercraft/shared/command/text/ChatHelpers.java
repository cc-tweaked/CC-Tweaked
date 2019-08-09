/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Various helpers for building chat messages
 */
public final class ChatHelpers
{
    private static final Formatting HEADER = Formatting.LIGHT_PURPLE;

    private ChatHelpers() {}

    public static Text coloured( String text, Formatting colour )
    {
        LiteralText component = new LiteralText( text == null ? "" : text );
        component.getStyle().setColor( colour );
        return component;
    }

    public static <T extends Text> T coloured( T component, Formatting colour )
    {
        component.getStyle().setColor( colour );
        return component;
    }

    public static Text text( String text )
    {
        return new LiteralText( text == null ? "" : text );
    }

    public static Text translate( String text )
    {
        return new TranslatableText( text == null ? "" : text );
    }

    public static Text translate( String text, Object... args )
    {
        return new TranslatableText( text == null ? "" : text, args );
    }

    public static Text list( Text... children )
    {
        Text component = new LiteralText( "" );
        for( Text child : children )
        {
            component.append( child );
        }
        return component;
    }

    public static Text position( BlockPos pos )
    {
        if( pos == null ) return translate( "commands.computercraft.generic.no_position" );
        return translate( "commands.computercraft.generic.position", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static Text bool( boolean value )
    {
        return value
            ? coloured( translate( "commands.computercraft.generic.yes" ), Formatting.GREEN )
            : coloured( translate( "commands.computercraft.generic.no" ), Formatting.RED );
    }

    public static Text link( Text component, String command, Text toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style.setColor( Formatting.YELLOW );
        style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, toolTip ) );

        return component;
    }

    public static Text header( String text )
    {
        return coloured( text, HEADER );
    }
}
