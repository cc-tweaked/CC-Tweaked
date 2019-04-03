/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.text.*;
import net.minecraft.text.event.ClickEvent;
import net.minecraft.text.event.HoverEvent;
import net.minecraft.util.math.BlockPos;

/**
 * Various helpers for building chat messages
 */
public final class ChatHelpers
{
    private static final TextFormat HEADER = TextFormat.LIGHT_PURPLE;

    private ChatHelpers() {}

    public static TextComponent coloured( String text, TextFormat colour )
    {
        TextComponent component = new StringTextComponent( text == null ? "" : text );
        component.getStyle().setColor( colour );
        return component;
    }

    public static <T extends TextComponent> T coloured( T component, TextFormat colour )
    {
        component.getStyle().setColor( colour );
        return component;
    }

    public static TextComponent text( String text )
    {
        return new StringTextComponent( text == null ? "" : text );
    }

    public static TextComponent translate( String text )
    {
        return new TranslatableTextComponent( text == null ? "" : text );
    }

    public static TextComponent translate( String text, Object... args )
    {
        return new TranslatableTextComponent( text == null ? "" : text, args );
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
        if( pos == null ) return translate( "commands.computercraft.generic.no_position" );
        return translate( "commands.computercraft.generic.position", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static TextComponent bool( boolean value )
    {
        return value
            ? coloured( translate( "commands.computercraft.generic.yes" ), TextFormat.GREEN )
            : coloured( translate( "commands.computercraft.generic.no" ), TextFormat.RED );
    }

    public static TextComponent link( TextComponent component, String command, TextComponent toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style.setColor( TextFormat.YELLOW );
        style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, toolTip ) );

        return component;
    }

    public static TextComponent header( String text )
    {
        return coloured( text, HEADER );
    }
}
