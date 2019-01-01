/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dan200.computercraft.shared.command.text.ChatHelpers;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A command which delegates to a series of sub commands
 */
public class CommandRoot implements ISubCommand
{
    private final String name;
    private final String synopsis;
    private final String description;
    private final Map<String, ISubCommand> subCommands = Maps.newHashMap();

    public CommandRoot( String name, String synopsis, String description )
    {
        this.name = name;
        this.synopsis = synopsis;
        this.description = description;

        register( new SubCommandHelp( this ) );
    }

    public void register( ISubCommand command )
    {
        subCommands.put( command.getName(), command );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Nonnull
    @Override
    public String getUsage( CommandContext context )
    {
        StringBuilder out = new StringBuilder( "<" );
        boolean first = true;
        for( ISubCommand command : subCommands.values() )
        {
            if( command.checkPermission( context ) )
            {
                if( first )
                {
                    first = false;
                }
                else
                {
                    out.append( "|" );
                }

                out.append( command.getName() );
            }
        }

        return out.append( ">" ).toString();
    }

    @Nonnull
    @Override
    public String getSynopsis()
    {
        return synopsis;
    }

    @Nonnull
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean checkPermission( @Nonnull CommandContext context )
    {
        for( ISubCommand command : subCommands.values() )
        {
            if( !(command instanceof SubCommandHelp) && command.checkPermission( context ) ) return true;
        }
        return false;
    }

    public Map<String, ISubCommand> getSubCommands()
    {
        return Collections.unmodifiableMap( subCommands );
    }

    @Override
    public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
    {
        if( arguments.size() == 0 )
        {
            context.getSender().sendMessage( ChatHelpers.getHelp( context, this, context.getFullPath() ) );
        }
        else
        {
            ISubCommand command = subCommands.get( arguments.get( 0 ) );
            if( command == null || !command.checkPermission( context ) )
            {
                throw new CommandException( context.getFullUsage() );
            }

            command.execute( context.enter( command ), arguments.subList( 1, arguments.size() ) );
        }
    }

    @Nonnull
    @Override
    public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
    {
        if( arguments.size() == 0 )
        {
            return Lists.newArrayList( subCommands.keySet() );
        }
        else if( arguments.size() == 1 )
        {
            List<String> list = Lists.newArrayList();
            String match = arguments.get( 0 );

            for( ISubCommand command : subCommands.values() )
            {
                if( CommandBase.doesStringStartWith( match, command.getName() ) && command.checkPermission( context ) )
                {
                    list.add( command.getName() );
                }
            }

            return list;
        }
        else
        {
            ISubCommand command = subCommands.get( arguments.get( 0 ) );
            if( command == null || !command.checkPermission( context ) ) return Collections.emptyList();

            return command.getCompletion( context, arguments.subList( 1, arguments.size() ) );
        }
    }
}
