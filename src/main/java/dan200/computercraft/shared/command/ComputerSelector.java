/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandException;

import java.util.*;
import java.util.function.Predicate;

final class ComputerSelector
{
    private ComputerSelector() {}

    private static List<ServerComputer> getComputers( Predicate<ServerComputer> predicate )
    {
        // We copy it to prevent concurrent modifications.
        ArrayList<ServerComputer> computers = new ArrayList<>( ComputerCraft.serverComputerRegistry.getComputers() );
        computers.removeIf( predicate.negate() );
        return computers;
    }

    static ServerComputer getComputer( String selector ) throws CommandException
    {
        List<ServerComputer> computers = getComputers( selector );
        if( computers.isEmpty() )
        {
            throw new CommandException( "commands.computercraft.argument.no_matching", selector );
        }
        else if( computers.size() == 1 )
        {
            return computers.get( 0 );
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            for( int i = 0; i < computers.size(); i++ )
            {
                if( i > 0 ) builder.append( ", " );
                builder.append( computers.get( i ).getInstanceID() );
            }

            throw new CommandException( "commands.computercraft.argument.many_matching", selector, builder.toString() );
        }
    }

    static List<ServerComputer> getComputers( String selector ) throws CommandException
    {
        if( !selector.isEmpty() && selector.charAt( 0 ) == '#' )
        {
            selector = selector.substring( 1 );

            int id;
            try
            {
                id = Integer.parseInt( selector );
            }
            catch( NumberFormatException e )
            {
                throw new CommandException( "commands.computercraft.argument.not_number", selector );
            }

            return getComputers( x -> x.getID() == id );
        }
        else if( !selector.isEmpty() && selector.charAt( 0 ) == '@' )
        {
            String label = selector.substring( 1 );
            return getComputers( x -> Objects.equals( label, x.getLabel() ) );
        }
        else if( !selector.isEmpty() && selector.charAt( 0 ) == '~' )
        {
            String familyName = selector.substring( 1 );
            return getComputers( x -> x.getFamily().name().equalsIgnoreCase( familyName ) );
        }
        else
        {
            int instance;
            try
            {
                instance = Integer.parseInt( selector );
            }
            catch( NumberFormatException e )
            {
                throw new CommandException( "commands.computercraft.argument.not_number", selector );
            }

            ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instance );
            return computer == null ? Collections.emptyList() : Collections.singletonList( computer );
        }
    }

    static List<String> completeComputer( String selector )
    {
        TreeSet<String> options = Sets.newTreeSet();

        // We copy it to prevent concurrent modifications.
        List<ServerComputer> computers = Lists.newArrayList( ComputerCraft.serverComputerRegistry.getComputers() );

        if( !selector.isEmpty() && selector.charAt( 0 ) == '#' )
        {
            selector = selector.substring( 1 );

            for( ServerComputer computer : computers )
            {
                String id = Integer.toString( computer.getID() );
                if( id.startsWith( selector ) ) options.add( "#" + id );
            }
        }
        else if( !selector.isEmpty() && selector.charAt( 0 ) == '@' )
        {
            String label = selector.substring( 1 );
            for( ServerComputer computer : computers )
            {
                String thisLabel = computer.getLabel();
                if( thisLabel != null && thisLabel.startsWith( label ) ) options.add( "@" + thisLabel );
            }
        }
        else if( !selector.isEmpty() && selector.charAt( 0 ) == '~' )
        {
            String familyName = selector.substring( 1 ).toLowerCase( Locale.ENGLISH );
            for( ComputerFamily family : ComputerFamily.values() )
            {
                if( family.name().toLowerCase( Locale.ENGLISH ).startsWith( familyName ) )
                {
                    options.add( "~" + family.name() );
                }
            }
        }
        else
        {
            for( ServerComputer computer : computers )
            {
                String id = Integer.toString( computer.getInstanceID() );
                if( id.startsWith( selector ) ) options.add( id );
            }
        }

        if( options.size() > 100 )
        {
            ArrayList<String> result = Lists.newArrayListWithCapacity( 100 );
            for( String element : options )
            {
                if( result.size() > 100 ) break;
                result.add( element );
            }

            return result;
        }
        else
        {
            return Lists.newArrayList( options );
        }
    }
}
