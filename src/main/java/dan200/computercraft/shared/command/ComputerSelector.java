package dan200.computercraft.shared.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandException;

import java.util.List;
import java.util.Set;

public final class ComputerSelector
{
    public static ServerComputer getComputer( String selector ) throws CommandException
    {
        if( selector.length() > 0 && selector.charAt( 0 ) == '#' )
        {
            selector = selector.substring( 1 );

            int id;
            try
            {
                id = Integer.parseInt( selector );
            }
            catch( NumberFormatException e )
            {
                throw new CommandException( "'" + selector + "' is not a valid number" );
            }

            // We copy it to prevent concurrent modifications.
            List<ServerComputer> computers = Lists.newArrayList( ComputerCraft.serverComputerRegistry.getComputers() );
            List<ServerComputer> candidates = Lists.newArrayList();
            for( ServerComputer searchComputer : computers )
            {
                if( searchComputer.getID() == id )
                {
                    candidates.add( searchComputer );
                }
            }

            if( candidates.size() == 0 )
            {
                throw new CommandException( "No such computer for id " + id );
            }
            else if( candidates.size() == 1 )
            {
                return candidates.get( 0 );
            }
            else
            {
                StringBuilder builder = new StringBuilder( "Multiple computers with id " )
                    .append( id ).append( " (instances " );

                boolean first = true;
                for( ServerComputer computer : candidates )
                {
                    if( first )
                    {
                        first = false;
                    }
                    else
                    {
                        builder.append( ", " );
                    }

                    builder.append( computer.getInstanceID() );
                }

                builder.append( ")" );

                throw new CommandException( builder.toString() );
            }
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
                throw new CommandException( "'" + selector + "' is not a valid number" );
            }

            ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instance );
            if( computer == null )
            {
                throw new CommandException( "No such computer for instance id " + instance );
            }
            else
            {
                return computer;
            }
        }
    }

    public static List<String> completeComputer( String selector )
    {
        Set<String> options = Sets.newHashSet();

        // We copy it to prevent concurrent modifications.
        List<ServerComputer> computers = Lists.newArrayList( ComputerCraft.serverComputerRegistry.getComputers() );

        if( selector.length() > 0 && selector.charAt( 0 ) == '#' )
        {
            selector = selector.substring( 1 );

            for( ServerComputer computer : computers )
            {
                String id = Integer.toString( computer.getID() );
                if( id.startsWith( selector ) ) options.add( "#" + id );
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

        return Lists.newArrayList( options );
    }
}
