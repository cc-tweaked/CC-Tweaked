/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.tracking.ComputerTracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingContext;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.Config;
import dan200.computercraft.shared.command.framework.*;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.Containers;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

import static dan200.computercraft.shared.command.text.ChatHelpers.*;

public final class CommandComputerCraft extends CommandDelegate
{
    public static final UUID SYSTEM_UUID = new UUID( 0, 0 );

    private static final int DUMP_LIST_ID = 5373952;
    private static final int DUMP_SINGLE_ID = 1844510720;
    private static final int TRACK_ID = 373882880;

    public CommandComputerCraft()
    {
        super( create() );
    }

    private static ISubCommand create()
    {
        CommandRoot root = new CommandRoot( "computercraft" );

        root.register( new SubCommandBase( "dump", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.isEmpty() )
                {
                    TableBuilder table = new TableBuilder( DUMP_LIST_ID, "Computer", "On", "Position" );

                    List<ServerComputer> computers = new ArrayList<>( ComputerCraft.serverComputerRegistry.getComputers() );

                    // Unless we're on a server, limit the number of rows we can send.
                    if( !(context.getSender() instanceof MinecraftServer) )
                    {
                        World world = context.getSender().getEntityWorld();
                        BlockPos pos = context.getSender().getPosition();

                        computers.sort( ( a, b ) -> {
                            if( a.getWorld() == b.getWorld() && a.getWorld() == world )
                            {
                                return Double.compare( a.getPosition().distanceSq( pos ), b.getPosition().distanceSq( pos ) );
                            }
                            else if( a.getWorld() == world )
                            {
                                return -1;
                            }
                            else if( b.getWorld() == world )
                            {
                                return 1;
                            }
                            else
                            {
                                return Integer.compare( a.getInstanceID(), b.getInstanceID() );
                            }
                        } );
                    }

                    for( ServerComputer computer : computers )
                    {
                        table.row(
                            linkComputer( context, computer, computer.getID() ),
                            bool( computer.isOn() ),
                            linkPosition( context, computer )
                        );
                    }

                    table.display( context.getSender() );
                }
                else if( arguments.size() == 1 )
                {
                    ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );

                    TableBuilder table = new TableBuilder( DUMP_SINGLE_ID );
                    table.row( header( "Instance" ), text( Integer.toString( computer.getInstanceID() ) ) );
                    table.row( header( "Id" ), text( Integer.toString( computer.getID() ) ) );
                    table.row( header( "Label" ), text( computer.getLabel() ) );
                    table.row( header( "On" ), bool( computer.isOn() ) );
                    table.row( header( "Position" ), linkPosition( context, computer ) );
                    table.row( header( "Family" ), text( computer.getFamily().toString() ) );

                    for( ComputerSide side : ComputerSide.values() )
                    {
                        IPeripheral peripheral = computer.getPeripheral( side );
                        if( peripheral != null )
                        {
                            table.row( header( "Peripheral " + side.getName() ), text( peripheral.getType() ) );
                        }
                    }

                    table.display( context.getSender() );
                }
                else
                {
                    throw new CommandException( context.getFullUsage() );
                }
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.size() == 1
                    ? ComputerSelector.completeComputer( arguments.get( 0 ) )
                    : Collections.emptyList();
            }
        } );

        root.register( new SubCommandBase( "shutdown", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                withComputers( arguments, computers -> {
                    int shutdown = 0;
                    for( ServerComputer computer : computers )
                    {
                        if( computer.isOn() ) shutdown++;
                        computer.shutdown();
                    }
                    context.getSender().sendMessage( translate( "commands.computercraft.shutdown.done", shutdown, computers.size() ) );
                } );
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.isEmpty()
                    ? Collections.emptyList()
                    : ComputerSelector.completeComputer( arguments.get( arguments.size() - 1 ) );
            }
        } );

        root.register( new SubCommandBase( "turn-on", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                withComputers( arguments, computers -> {
                    int on = 0;
                    for( ServerComputer computer : computers )
                    {
                        if( !computer.isOn() ) on++;
                        computer.turnOn();
                    }
                    context.getSender().sendMessage( translate( "commands.computercraft.turn_on.done", on, computers.size() ) );
                } );
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.isEmpty()
                    ? Collections.emptyList()
                    : ComputerSelector.completeComputer( arguments.get( arguments.size() - 1 ) );
            }
        } );

        root.register( new SubCommandBase( "tp", UserLevel.OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() != 1 ) throw new CommandException( context.getFullUsage() );

                ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );
                World world = computer.getWorld();
                BlockPos pos = computer.getPosition();

                if( world == null || pos == null ) throw new CommandException( "commands.computercraft.tp.not_there" );

                ICommandSender sender = context.getSender();
                if( !(sender instanceof Entity) ) throw new CommandException( "commands.computercraft.tp.not_entity" );

                if( sender instanceof EntityPlayerMP )
                {
                    EntityPlayerMP entity = (EntityPlayerMP) sender;
                    if( entity.getEntityWorld() != world )
                    {
                        context.getServer().getPlayerList().changePlayerDimension( entity, world.provider.getDimension() );
                    }

                    entity.setPositionAndUpdate( pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5 );
                }
                else
                {
                    Entity entity = (Entity) sender;
                    if( entity.getEntityWorld() != world )
                    {
                        entity.changeDimension( world.provider.getDimension() );
                    }

                    entity.setLocationAndAngles(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        entity.rotationYaw, entity.rotationPitch
                    );
                }
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.size() == 1
                    ? ComputerSelector.completeComputer( arguments.get( 0 ) )
                    : Collections.emptyList();
            }
        } );

        root.register( new SubCommandBase( "view", UserLevel.OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() != 1 ) throw new CommandException( context.getFullUsage() );

                ICommandSender sender = context.getSender();
                if( !(sender instanceof EntityPlayerMP) )
                {
                    throw new CommandException( "commands.computercraft.view.not_player" );
                }

                ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );
                Containers.openComputerGUI( (EntityPlayerMP) sender, computer );
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.size() == 1
                    ? ComputerSelector.completeComputer( arguments.get( 0 ) )
                    : Collections.emptyList();
            }
        } );

        root.register( new CommandRoot( "track" ).register( new SubCommandBase( "start", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                getTimingContext( context ).start();

                String stopCommand = "/" + context.parent().getFullPath() + " stop";
                context.getSender().sendMessage( list(
                    translate( "commands.computercraft.track.start.stop",
                        link( text( stopCommand ), stopCommand, translate( "commands.computercraft.track.stop.action" ) ) )
                ) );
            }
        } ).register( new SubCommandBase( "stop", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                TrackingContext timings = getTimingContext( context );
                if( !timings.stop() ) throw new CommandException( "commands.computercraft.track.stop.not_enabled" );
                displayTimings( context, timings.getImmutableTimings(), TrackingField.AVERAGE_TIME );
            }
        } ).register( new SubCommandBase( "dump", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                TrackingField field = TrackingField.AVERAGE_TIME;
                if( arguments.size() >= 1 )
                {
                    field = TrackingField.fields().get( arguments.get( 0 ) );
                    if( field == null )
                    {
                        throw new CommandException( "commands.computercraft.track.dump.no_field", arguments.get( 0 ) );
                    }
                }

                displayTimings( context, getTimingContext( context ).getImmutableTimings(), field );
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                if( arguments.size() == 1 )
                {
                    String match = arguments.get( 0 );

                    List<String> out = new ArrayList<>();
                    for( String key : TrackingField.fields().keySet() )
                    {
                        if( CommandBase.doesStringStartWith( match, key ) ) out.add( key );
                    }

                    out.sort( Comparator.naturalOrder() );
                    return out;
                }
                else
                {
                    return super.getCompletion( context, arguments );
                }
            }
        } ) );

        root.register( new SubCommandBase( "reload", UserLevel.OWNER_OP )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                Config.reload();
                context.getSender().sendMessage( translate( "commands.computercraft.reload.done" ) );
            }
        } );

        root.register( new SubCommandBase( "queue", UserLevel.ANYONE )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() < 1 ) throw new CommandException( context.getFullUsage() );

                String selector = arguments.get( 0 );
                Object[] rest = arguments.subList( 1, arguments.size() ).toArray();

                boolean found = false;
                for( ServerComputer computer : ComputerSelector.getComputers( selector ) )
                {
                    if( computer.getFamily() != ComputerFamily.Command || !computer.isOn() ) continue;
                    found = true;
                    computer.queueEvent( "computer_command", rest );
                }

                if( !found )
                {
                    throw new CommandException( "commands.computercraft.argument.no_matching", selector );
                }
            }
        } );

        return root;
    }

    private static ITextComponent linkComputer( CommandContext context, ServerComputer serverComputer, int computerId )
    {
        ITextComponent out = new TextComponentString( "" );

        // Append the computer instance
        if( serverComputer == null )
        {
            out.appendSibling( text( "?" ) );
        }
        else
        {
            out.appendSibling( link(
                text( Integer.toString( serverComputer.getInstanceID() ) ),
                "/computercraft dump " + serverComputer.getInstanceID(),
                translate( "commands.computercraft.dump.action" )
            ) );
        }

        // And ID
        out.appendText( " (id " + computerId + ")" );

        // And, if we're a player, some useful links
        if( serverComputer != null && UserLevel.OP.canExecute( context ) && context.fromPlayer() )
        {
            out
                .appendText( " " )
                .appendSibling( link(
                    text( "\u261b" ),
                    "/computercraft tp " + serverComputer.getInstanceID(),
                    translate( "commands.computercraft.tp.action" )
                ) )
                .appendText( " " )
                .appendSibling( link(
                    text( "\u20e2" ),
                    "/computercraft view " + serverComputer.getInstanceID(),
                    translate( "commands.computercraft.view.action" )
                ) );
        }

        return out;
    }

    private static ITextComponent linkPosition( CommandContext context, ServerComputer computer )
    {
        if( UserLevel.OP.canExecute( context ) )
        {
            return link(
                position( computer.getPosition() ),
                "/computercraft tp " + computer.getInstanceID(),
                translate( "commands.computercraft.tp.action" )
            );
        }
        else
        {
            return position( computer.getPosition() );
        }
    }

    private static TrackingContext getTimingContext( CommandContext context )
    {
        Entity entity = context.getSender().getCommandSenderEntity();
        if( entity instanceof EntityPlayerMP )
        {
            return Tracking.getContext( entity.getUniqueID() );
        }
        else
        {
            return Tracking.getContext( SYSTEM_UUID );
        }
    }

    private static void displayTimings( CommandContext context, List<ComputerTracker> timings, TrackingField field ) throws CommandException
    {
        if( timings.isEmpty() ) throw new CommandException( "commands.computercraft.track.dump.no_timings" );

        Map<Computer, ServerComputer> lookup = new HashMap<>();
        int maxId = 0, maxInstance = 0;
        for( ServerComputer server : ComputerCraft.serverComputerRegistry.getComputers() )
        {
            lookup.put( server.getComputer(), server );

            if( server.getInstanceID() > maxInstance ) maxInstance = server.getInstanceID();
            if( server.getID() > maxId ) maxId = server.getID();
        }

        timings.sort( Comparator.<ComputerTracker, Long>comparing( x -> x.get( field ) ).reversed() );

        boolean defaultLayout = field == TrackingField.TASKS || field == TrackingField.TOTAL_TIME
            || field == TrackingField.AVERAGE_TIME || field == TrackingField.MAX_TIME;


        TableBuilder table = defaultLayout ? new TableBuilder(
            TRACK_ID,
            translate( "commands.computercraft.track.dump.computer" ),
            translate( TrackingField.TASKS.translationKey() ),
            translate( TrackingField.TOTAL_TIME.translationKey() ),
            translate( TrackingField.AVERAGE_TIME.translationKey() ),
            translate( TrackingField.MAX_TIME.translationKey() )
        ) : new TableBuilder(
            TRACK_ID,
            translate( "commands.computercraft.track.dump.computer" ),
            translate( field.translationKey() )
        );

        for( ComputerTracker entry : timings )
        {
            Computer computer = entry.getComputer();
            ServerComputer serverComputer = computer == null ? null : lookup.get( computer );

            ITextComponent computerComponent = linkComputer( context, serverComputer, entry.getComputerId() );

            if( defaultLayout )
            {
                table.row(
                    computerComponent,
                    text( entry.getFormatted( TrackingField.TASKS ) ),
                    text( entry.getFormatted( TrackingField.TOTAL_TIME ) ),
                    text( entry.getFormatted( TrackingField.AVERAGE_TIME ) ),
                    text( entry.getFormatted( TrackingField.MAX_TIME ) )
                );
            }
            else
            {
                table.row( computerComponent, text( entry.getFormatted( field ) ) );
            }
        }

        table.display( context.getSender() );
    }

    private static void withComputers( List<String> selectors, Consumer<Collection<ServerComputer>> action ) throws CommandException
    {
        Set<ServerComputer> computers = Sets.newHashSet();
        List<String> failed = new ArrayList<>();
        if( selectors.isEmpty() )
        {
            computers.addAll( ComputerCraft.serverComputerRegistry.getComputers() );
        }
        else
        {
            for( String selector : selectors )
            {
                List<ServerComputer> selected = ComputerSelector.getComputers( selector );
                computers.addAll( selected );
                if( selected.isEmpty() ) failed.add( selector );
            }
        }

        action.accept( computers );

        if( !failed.isEmpty() )
        {
            throw new CommandException( "commands.computercraft.argument.no_matching", String.join( ", ", failed ) );
        }
    }
}
