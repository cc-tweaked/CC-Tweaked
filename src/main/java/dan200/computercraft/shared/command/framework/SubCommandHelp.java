/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import dan200.computercraft.shared.command.text.ChatHelpers;
import joptsimple.internal.Strings;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

class SubCommandHelp implements ISubCommand
{
    private final CommandRoot branchCommand;

    SubCommandHelp( CommandRoot branchCommand )
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
    public String getFullName()
    {
        return "computercraft.help";
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
                throw new CommandException( "commands.computercraft.help.no_children", Strings.join( arguments.subList( 0, i ), " " ) );
            }

            if( command == null )
            {
                throw new CommandException( "commands.computercraft.help.no_command", Strings.join( arguments.subList( 0, i + 1 ), "  " ) );
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

        if( arguments.isEmpty() )
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
