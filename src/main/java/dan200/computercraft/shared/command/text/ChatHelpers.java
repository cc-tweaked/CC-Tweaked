/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/**
 * Various helpers for building chat messages
 */
public final class ChatHelpers
{
    private static final TextFormatting HEADER = TextFormatting.LIGHT_PURPLE;

    public static ITextComponent coloured( String text, TextFormatting colour )
    {
        ITextComponent component = new TextComponentString( text == null ? "" : text );
        component.getStyle().setColor( colour );
        return component;
    }

    public static ITextComponent text( String text )
    {
        return new TextComponentString( text == null ? "" : text );
    }

    public static ITextComponent list( ITextComponent... children )
    {
        ITextComponent component = new TextComponentString( "" );
        for( ITextComponent child : children )
        {
            component.appendSibling( child );
        }
        return component;
    }

    public static ITextComponent position( BlockPos pos )
    {
        if( pos == null ) return text( "<no pos>" );
        return formatted( "%d, %d, %d", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static ITextComponent bool( boolean value )
    {
        if( value )
        {
            ITextComponent component = new TextComponentString( "Y" );
            component.getStyle().setColor( TextFormatting.GREEN );
            return component;
        }
        else
        {
            ITextComponent component = new TextComponentString( "N" );
            component.getStyle().setColor( TextFormatting.RED );
            return component;
        }
    }

    public static ITextComponent formatted( String format, Object... args )
    {
        return new TextComponentString( String.format( format, args ) );
    }

    public static ITextComponent link( ITextComponent component, String command, String toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style.setColor( TextFormatting.YELLOW );
        style.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ) );
        style.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new TextComponentString( toolTip ) ) );

        return component;
    }

    public static ITextComponent header( String text )
    {
        return coloured( text, HEADER );
    }
}
