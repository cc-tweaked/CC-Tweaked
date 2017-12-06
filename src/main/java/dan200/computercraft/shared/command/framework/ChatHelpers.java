package dan200.computercraft.shared.command.framework;

import com.google.common.base.Strings;
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
    private static final TextFormatting SYNOPSIS = TextFormatting.AQUA;
    private static final TextFormatting NAME = TextFormatting.GREEN;

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

    public static ITextComponent getHelp( CommandContext context, ISubCommand command, String prefix )
    {
        ITextComponent output = new TextComponentString( "" )
            .appendSibling( coloured( "/" + prefix + " " + command.getUsage( context ), HEADER ) )
            .appendText( " " )
            .appendSibling( coloured( command.getSynopsis(), SYNOPSIS ) );

        String desc = command.getDescription();
        if( !Strings.isNullOrEmpty( desc ) ) output.appendText( "\n" + desc );

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

                output.appendText( " - " + subCommand.getSynopsis() );
            }
        }

        return output;
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
