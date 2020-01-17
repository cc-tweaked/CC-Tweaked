/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.text;

import dan200.computercraft.shared.command.framework.CommandContext;
import dan200.computercraft.shared.command.framework.CommandRoot;
import dan200.computercraft.shared.command.framework.ISubCommand;
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
    private static final TextFormatting SYNOPSIS = TextFormatting.AQUA;
    private static final TextFormatting NAME = TextFormatting.GREEN;

    private ChatHelpers() {}

    public static ITextComponent coloured( String text, TextFormatting colour )
    {
        ITextComponent component = new TextComponentString( text == null ? "" : text );
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
        return new TextComponentString( text == null ? "" : text );
    }

    public static ITextComponent translate( String text )
    {
        return new TextComponentTranslation( text == null ? "" : text );
    }

    public static ITextComponent translate( String text, Object... args )
    {
        return new TextComponentTranslation( text == null ? "" : text, args );
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

    public static ITextComponent getHelp( CommandContext context, ISubCommand command, String prefix )
    {

        ITextComponent output = new TextComponentString( "" )
            .appendSibling(
                coloured( "/" + prefix, HEADER )
                    .appendSibling( translate( command.getUsage( context ) ) )
            )
            .appendText( " " )
            .appendSibling( coloured( translate( "commands." + command.getFullName() + ".synopsis" ), SYNOPSIS ) )
            .appendText( "\n" )
            .appendSibling( translate( "commands." + command.getFullName() + ".desc" ) );

        if( command instanceof CommandRoot )
        {
            for( ISubCommand subCommand : ((CommandRoot) command).getSubCommands().values() )
            {
                if( !subCommand.checkPermission( context ) ) continue;

                output.appendText( "\n" );

                ITextComponent component = coloured( subCommand.getName(), NAME );
                component.getStyle().setClickEvent( new ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    "/" + prefix + " " + subCommand.getName()
                ) );
                output.appendSibling( component );

                output.appendText( " - " ).appendSibling( translate( "commands." + subCommand.getFullName() + ".synopsis" ) );
            }
        }

        return output;
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
