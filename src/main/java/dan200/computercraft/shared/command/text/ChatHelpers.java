/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.text;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/**
 * Various helpers for building chat messages.
 */
public final class ChatHelpers
{
    private static final TextFormatting HEADER = TextFormatting.LIGHT_PURPLE;

    private ChatHelpers() {}

    public static ITextComponent coloured( String text, TextFormatting colour )
    {
        ITextComponent component = new StringTextComponent( text == null ? "" : text );
        component.getStyle().setColor( colour );
        return component;
    }

    public static <T extends ITextComponent> T coloured( T component, TextFormatting colour )
    {
        component.getStyle().setColor( colour );
        return component;
    }

    public static ITextComponent text( String text )
    {
        return new StringTextComponent( text == null ? "" : text );
    }

    public static ITextComponent translate( String text )
    {
        return new TranslationTextComponent( text == null ? "" : text );
    }

    public static ITextComponent translate( String text, Object... args )
    {
        return new TranslationTextComponent( text == null ? "" : text, args );
    }

    public static ITextComponent list( ITextComponent... children )
    {
        ITextComponent component = new StringTextComponent( "" );
        for( ITextComponent child : children )
        {
            component.append( child );
        }
        return component;
    }

    public static ITextComponent position( BlockPos pos )
    {
        if( pos == null ) return translate( "commands.computercraft.generic.no_position" );
        return translate( "commands.computercraft.generic.position", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static ITextComponent bool( boolean value )
    {
        return value
            ? coloured( translate( "commands.computercraft.generic.yes" ), TextFormatting.GREEN )
            : coloured( translate( "commands.computercraft.generic.no" ), TextFormatting.RED );
    }

    public static ITextComponent link( ITextComponent component, String command, ITextComponent toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style.setColor( TextFormatting.YELLOW );
        style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, toolTip ) );

        return component;
    }

    public static ITextComponent header( String text )
    {
        return coloured( text, HEADER );
    }
}
