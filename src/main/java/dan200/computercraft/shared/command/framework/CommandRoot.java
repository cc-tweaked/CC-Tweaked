/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
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
 * A command which delegates to a series of sub commands.
 */
public class CommandRoot implements ISubCommand
{
    private final String name;
    private ISubCommand parent;
    private final Map<String, ISubCommand> subCommands = Maps.newHashMap();

    public CommandRoot( String name )
    {
        this.name = name;
        register( new SubCommandHelp( this ) );
    }

    public CommandRoot register( ISubCommand command )
    {
        subCommands.put( command.getName(), command );
        if( command instanceof SubCommandBase )
        {
            ((SubCommandBase) command).setParent( this );
        }
        else if( command instanceof CommandRoot )
        {
            ((CommandRoot) command).setParent( this );
        }

        return this;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Nonnull
    @Override
    public String getFullName()
    {
        return parent == null ? name : parent.getFullName() + "." + name;
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
        if( arguments.isEmpty() )
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
        if( arguments.isEmpty() )
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

    void setParent( @Nonnull ISubCommand parent )
    {
        if( this.parent != null ) throw new IllegalStateException( "Cannot have multiple parents" );
        this.parent = parent;
    }
}
