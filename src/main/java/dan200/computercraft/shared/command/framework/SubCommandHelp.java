package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import joptsimple.internal.Strings;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class SubCommandHelp implements ISubCommand
{
    private final CommandRoot branchCommand;

    public SubCommandHelp( CommandRoot branchCommand )
    {
        this.branchCommand = branchCommand;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "help";
    }

    @Nonnull
    @Override
    public String getUsage( CommandContext context )
    {
        return "[command]";
    }

    @Nonnull
    @Override
    public String getSynopsis()
    {
        return "Provide help for a specific command";
    }

    @Nonnull
    @Override
    public String getDescription()
    {
        return "";
    }

    @Override
    public boolean checkPermission( @Nonnull CommandContext context )
    {
        return true;
    }

    @Override
    public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
    {
        ISubCommand command = branchCommand;

        for( int i = 0; i < arguments.size(); i++ )
        {
            String commandName = arguments.get( i );
            if( command instanceof CommandRoot )
            {
                command = ((CommandRoot) command).getSubCommands().get( commandName );
            }
            else
            {
                throw new CommandException( Strings.join( arguments.subList( 0, i ), " " ) + " has no sub-commands" );
            }

            if( command == null )
            {
                throw new CommandException( "No such command " + Strings.join( arguments.subList( 0, i + 1 ), "  " ) );
            }
        }

        StringBuilder prefix = new StringBuilder( context.parent().getFullPath() );
        for( String argument : arguments )
        {
            prefix.append( ' ' ).append( argument );
        }
        context.getSender().sendMessage( ChatHelpers.getHelp( context, command, prefix.toString() ) );
    }

    @Nonnull
    @Override
    public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
    {
        CommandRoot command = branchCommand;

        for( int i = 0; i < arguments.size() - 1; i++ )
        {
            String commandName = arguments.get( i );
            ISubCommand subCommand = command.getSubCommands().get( commandName );

            if( subCommand instanceof CommandRoot )
            {
                command = (CommandRoot) subCommand;
            }
            else
            {
                return Collections.emptyList();
            }
        }

        if( arguments.size() == 0 )
        {
            return Lists.newArrayList( command.getSubCommands().keySet() );
        }
        else
        {
            List<String> list = Lists.newArrayList();
            String match = arguments.get( arguments.size() - 1 );

            for( String entry : command.getSubCommands().keySet() )
            {
                if( CommandBase.doesStringStartWith( match, entry ) )
                {
                    list.add( entry );
                }
            }

            return list;
        }
    }
}
