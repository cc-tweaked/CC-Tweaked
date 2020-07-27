/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
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

    public static IFormattableTextComponent coloured( String text, TextFormatting colour )
    {
        return new StringTextComponent( text == null ? "" : text ).mergeStyle( colour );
    }

    public static <T extends IFormattableTextComponent> T coloured( T component, TextFormatting colour )
    {
        component.mergeStyle( colour );
        return component;
    }

    public static IFormattableTextComponent text( String text )
    {
        return new StringTextComponent( text == null ? "" : text );
    }

    public static IFormattableTextComponent translate( String text )
    {
        return new TranslationTextComponent( text == null ? "" : text );
    }

    public static IFormattableTextComponent translate( String text, Object... args )
    {
        return new TranslationTextComponent( text == null ? "" : text, args );
    }

    public static IFormattableTextComponent list( ITextComponent... children )
    {
        IFormattableTextComponent component = new StringTextComponent( "" );
        for( ITextComponent child : children )
        {
            component.append( child );
        }
        return component;
    }

    public static IFormattableTextComponent position( BlockPos pos )
    {
        if( pos == null ) return translate( "commands.computercraft.generic.no_position" );
        return translate( "commands.computercraft.generic.position", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static IFormattableTextComponent bool( boolean value )
    {
        return value
            ? coloured( translate( "commands.computercraft.generic.yes" ), TextFormatting.GREEN )
            : coloured( translate( "commands.computercraft.generic.no" ), TextFormatting.RED );
    }

    public static IFormattableTextComponent link( IFormattableTextComponent component, String command, ITextComponent toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style = style.setFormatting( TextFormatting.YELLOW );
        style = style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style = style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, toolTip ) );

        return component.setStyle( style );
    }

    public static IFormattableTextComponent header( String text )
    {
        return coloured( text, HEADER );
    }
}
