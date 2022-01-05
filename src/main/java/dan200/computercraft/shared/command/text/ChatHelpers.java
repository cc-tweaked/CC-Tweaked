/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.text;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Various helpers for building chat messages.
 */
public final class ChatHelpers
{
    private static final Formatting HEADER = Formatting.LIGHT_PURPLE;

    private ChatHelpers() {}

    public static MutableText coloured( String text, Formatting colour )
    {
        MutableText component = new LiteralText( text == null ? "" : text );
        component.setStyle( component.getStyle().withColor( colour ) );
        return component;
    }

    public static <T extends MutableText> T coloured( T component, Formatting colour )
    {
        component.setStyle( component.getStyle().withColor( colour ) );
        return component;
    }

    public static MutableText text( String text )
    {
        return new LiteralText( text == null ? "" : text );
    }

    public static MutableText translate( String text )
    {
        return new TranslatableText( text == null ? "" : text );
    }

    public static MutableText translate( String text, Object... args )
    {
        return new TranslatableText( text == null ? "" : text, args );
    }

    public static MutableText list( MutableText... children )
    {
        MutableText component = new LiteralText( "" );
        for( MutableText child : children )
        {
            component.append( child );
        }
        return component;
    }

    public static MutableText position( BlockPos pos )
    {
        if( pos == null ) return translate( "commands.computercraft.generic.no_position" );
        return translate( "commands.computercraft.generic.position", pos.getX(), pos.getY(), pos.getZ() );
    }

    public static MutableText bool( boolean value )
    {
        return value
            ? coloured( translate( "commands.computercraft.generic.yes" ), Formatting.GREEN )
            : coloured( translate( "commands.computercraft.generic.no" ), Formatting.RED );
    }

    public static MutableText link( MutableText component, String command, MutableText toolTip )
    {
        return link( component, new ClickEvent( ClickEvent.Action.RUN_COMMAND, command ), toolTip );
    }

    public static MutableText link( MutableText component, ClickEvent click, MutableText toolTip )
    {
        Style style = component.getStyle();

        if( style.getColor() == null ) style = style.withColor( Formatting.YELLOW );
        style = style.withClickEvent( click );
        style = style.withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, toolTip ) );

        component.setStyle( style );

        return component;
    }

    public static MutableText header( String text )
    {
        return coloured( text, HEADER );
    }

    public static MutableText copy( String text )
    {
        LiteralText name = new LiteralText( text );
        name.setStyle( name.getStyle()
            .withClickEvent( new ClickEvent( ClickEvent.Action.COPY_TO_CLIPBOARD, text ) )
            .withHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new TranslatableText( "gui.computercraft.tooltip.copy" ) ) ) );
        return name;
    }
}
