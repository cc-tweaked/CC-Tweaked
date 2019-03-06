/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.text.StringTextComponent;
import net.minecraft.text.Style;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TextFormat;
import net.minecraft.text.event.ClickEvent;
import net.minecraft.text.event.HoverEvent;
import net.minecraft.util.math.BlockPos;

/**
 * Various helpers for building chat messages
 */
public final class ChatHelpers
{
    private static final TextFormat HEADER = TextFormat.LIGHT_PURPLE;

    public static TextComponent coloured( String text, TextFormat colour )
    {
        TextComponent component = new StringTextComponent( text == null ? "" : text );
        component.getStyle().setColor( colour );
        return component;
    }

    public static TextComponent text( String text )
    {
        return new StringTextComponent( text == null ? "" : text );
    }

    public static TextComponent list( TextComponent... children )
    {
        TextComponent component = new StringTextComponent( "" );
        for( TextComponent child : children )
        {
            component.append( child );
        }
        return component;
    }

    public static TextComponent position( BlockPos pos )
    {
        if( pos == null ) return text( "<no pos>" );
        return formatted( "%d, %d, %d", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static TextComponent bool( boolean value )
    {
        if( value )
        {
            TextComponent component = new StringTextComponent( "Y" );
            component.getStyle().setColor( TextFormat.GREEN );
            return component;
        }
        else
        {
            TextComponent component = new StringTextComponent( "N" );
            component.getStyle().setColor( TextFormat.RED );
            return component;
        }
    }

    public static TextComponent formatted( String format, Object... args )
    {
        return new StringTextComponent( String.format( format, args ) );
    }

    public static TextComponent link( TextComponent component, String command, String toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style.setColor( TextFormat.YELLOW );
        style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new StringTextComponent( toolTip ) ) );

        return component;
    }

    public static TextComponent header( String text )
    {
        return coloured( text, HEADER );
    }
}
