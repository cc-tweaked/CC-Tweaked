/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.tracking.ComputerTracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingContext;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;

import static dan200.computercraft.shared.command.CommandUtils.isPlayer;
import static dan200.computercraft.shared.command.Exceptions.*;
import static dan200.computercraft.shared.command.arguments.ComputerArgumentType.getComputerArgument;
import static dan200.computercraft.shared.command.arguments.ComputerArgumentType.oneComputer;
import static dan200.computercraft.shared.command.arguments.ComputersArgumentType.*;
import static dan200.computercraft.shared.command.arguments.TrackingFieldArgumentType.trackingField;
import static dan200.computercraft.shared.command.builder.CommandBuilder.args;
import static dan200.computercraft.shared.command.builder.CommandBuilder.command;
import static dan200.computercraft.shared.command.builder.DescribedArgumentBuilder.literal;
import static dan200.computercraft.shared.command.text.ChatHelpers.*;

public final class CommandComputerCraft
{
    public static final UUID SYSTEM_UUID = new UUID( 0, 0 );

    private static final int DUMP_LIST_ID = 5373952;
    private static final int DUMP_SINGLE_ID = 1844510720;
    private static final int TRACK_ID = 373882880;

    private CommandComputerCraft()
    {
    }

    public static void register( CommandDispatcher<ServerCommandSource> dispatcher )
    {
        dispatcher.register( literal( "computercraft",
            "The /computercraft command provides various debugging and administrator tools for controlling and" +
                "interacting with computers." )
            .then( literal( "dump", "Display the status of all computers or specific information about one computer." )
                .requires( UserLevel.OWNER_OP )
                .executes( context -> {
                    TableBuilder table = new TableBuilder( DUMP_LIST_ID, "Computer", "On", "Position" );

                    ServerCommandSource source = context.getSource();
                    List<ServerComputer> computers = new ArrayList<>( ComputerCraft.serverComputerRegistry.getComputers() );

                    // Unless we're on a server, limit the number of rows we can send.
                    World world = source.getWorld();
                    BlockPos pos = new BlockPos( source.getPosition() );

                    computers.sort( ( a, b ) -> {
                        if( a.getWorld() == b.getWorld() && a.getWorld() == world )
                        {
                            return Double.compare( a.getPosition().squaredDistanceTo( pos ), b.getPosition().squaredDistanceTo( pos ) );
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

                    for( ServerComputer computer : computers )
                    {
                        table.row(
                            linkComputer( source, computer, computer.getID() ),
                            bool( computer.isOn() ),
                            linkPosition( source, computer )
                        );
                    }

                    table.display( context.getSource() );
                    return computers.size();
                } )
                .then( args()
                    .arg( "computer", oneComputer() )
                    .executes( context -> {
                        ServerComputer computer = getComputerArgument( context, "computer" );

                        TableBuilder table = new TableBuilder( DUMP_SINGLE_ID );
                        table.row( header( "Instance" ), text( Integer.toString( computer.getInstanceID() ) ) );
                        table.row( header( "Id" ), text( Integer.toString( computer.getID() ) ) );
                        table.row( header( "Label" ), text( computer.getLabel() ) );
                        table.row( header( "On" ), bool( computer.isOn() ) );
                        table.row( header( "Position" ), linkPosition( context.getSource(), computer ) );
                        table.row( header( "Family" ), text( computer.getFamily().toString() ) );

                        for( int i = 0; i < 6; i++ )
                        {
                            IPeripheral peripheral = computer.getPeripheral( i );
                            if( peripheral != null )
                            {
                                table.row( header( "Peripheral " + IAPIEnvironment.SIDE_NAMES[i] ), text( peripheral.getType() ) );
                            }
                        }

                        table.display( context.getSource() );
                        return 1;
                    } ) ) )

            .then( command( "shutdown", "Shutdown the specified computers." )
                .requires( UserLevel.OWNER_OP )
                .argManyValue( "computers", manyComputers(), s -> new ArrayList<>( ComputerCraft.serverComputerRegistry.getComputers() ) )
                .executes( ( context, computers ) -> {
                    int shutdown = 0;
                    for( ServerComputer computer : unwrap( context.getSource(), computers ) )
                    {
                        if( computer.isOn() ) shutdown++;
                        computer.unload();
                    }
                    context.getSource().sendFeedback( text( "Shutdown " + shutdown + " / " + computers.size() + " computers" ), false );
                    return shutdown;
                } ) )

            .then( command( "turn-on", "Turn on the specified computers." )
                .requires( UserLevel.OWNER_OP )
                .argManyValue( "computers", manyComputers(), s -> new ArrayList<>( ComputerCraft.serverComputerRegistry.getComputers() ) )
                .executes( ( context, computers ) -> {
                    int turnedOn = 0;
                    for( ServerComputer computer : unwrap( context.getSource(), computers ) )
                    {
                        if( !computer.isOn() ) turnedOn++;
                        computer.turnOn();
                    }
                    context.getSource().sendFeedback( text( "Turned on " + turnedOn + " / " + computers.size() + " computers" ), false );
                    return turnedOn;
                } ) )

            .then( command( "tp", "Teleport to a specific computer." )
                .requires( UserLevel.OP )
                .arg( "computer", oneComputer() )
                .executes( context -> {
                    ServerComputer computer = getComputerArgument( context, "computer" );
                    World world = computer.getWorld();
                    BlockPos pos = computer.getPosition();

                    if( world == null || pos == null ) throw UNLOCATED_COMPUTER_EXCEPTION.create();

                    // TODO: Abstract all of this into some shared logic. See also TeleportCommand#method_13766
                    Entity entity = context.getSource().getEntityOrThrow();
                    if( entity instanceof ServerPlayerEntity )
                    {
                        ServerPlayerEntity player = (ServerPlayerEntity) entity;
                        if( player.getEntityWorld() == world )
                        {
                            player.networkHandler.teleportRequest( pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0 );
                        }
                        else
                        {
                            player.method_14251( (ServerWorld) world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0 );
                        }
                    }
                    else
                    {
                        context.getSource().sendError( new StringTextComponent( "Cannot teleport non-player to computer" ) );
                    }

                    return 1;
                } ) )

            .then( command( "queue",
                "Send a computer_command event to a command computer, passing through the additional arguments. " +
                    "This is mostly designed for map makers, acting as a more computer-friendly version of /trigger. " +
                    "Any player can run the command, which would most likely be done through a text component's " +
                    "click event." )
                .requires( UserLevel.ANYONE )
                .arg( "computer", manyComputers() )
                .argManyValue( "args", StringArgumentType.string(), Collections.emptyList() )
                .executes( ( ctx, args ) -> {
                    Collection<ServerComputer> computers = getComputersArgument( ctx, "computer" );
                    Object[] rest = args.toArray();

                    int queued = 0;
                    for( ServerComputer computer : computers )
                    {
                        if( computer.getFamily() == ComputerFamily.Command && computer.isOn() )
                        {
                            computer.queueEvent( "computer_command", rest );
                            queued++;
                        }
                    }

                    return queued;
                } ) )

            .then( command( "view",
                "Open the terminal of a computer, allowing remote control of a computer. This does not provide " +
                    "access to a turtle's inventory." )
                .requires( UserLevel.OP )
                .arg( "computer", oneComputer() )
                .executes( context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    ServerComputer computer = getComputerArgument( context, "computer" );
                    ComputerCraft.openComputerGUI( player, computer );
                    return 1;
                } ) )

            .then( literal( "track",
                "Track how long computers execute for, as well as how many events they handle. This presents " +
                    "information in a similar way to /forge track and can be useful for diagnosing lag." )
                .then( command( "start",
                    "Start tracking all computers' execution times and event counts. This will discard the " +
                        "results of previous runs." )
                    .requires( UserLevel.OWNER_OP )
                    .executes( context -> {
                        getTimingContext( context.getSource() ).start();

                        String stopCommand = "/computercraft track stop";
                        context.getSource().sendFeedback( list(
                            text( "Run " ),
                            link( text( stopCommand ), stopCommand, "Click to stop tracking" ),
                            text( " to stop tracking and view the results" )
                        ), false );
                        return 1;
                    } ) )

                .then( command( "stop", "Stop tracking all computers' events and execution times" )
                    .requires( UserLevel.OWNER_OP )
                    .executes( context -> {
                        TrackingContext timings = getTimingContext( context.getSource() );
                        if( !timings.stop() ) throw NOT_TRACKING_EXCEPTION.create();
                        displayTimings( context.getSource(), timings.getImmutableTimings(), TrackingField.AVERAGE_TIME, DEFAULT_FIELDS );
                        return 1;
                    } ) )

                .then( command( "dump", "Dump the latest results of computer tracking." )
                    .requires( UserLevel.OWNER_OP )
                    .argManyValue( "fields", trackingField(), DEFAULT_FIELDS )
                    .executes( ( context, fields ) -> {
                        TrackingField sort;
                        if( fields.size() == 1 && DEFAULT_FIELDS.contains( fields.get( 0 ) ) )
                        {
                            sort = fields.get( 0 );
                            fields = DEFAULT_FIELDS;
                        }
                        else
                        {
                            sort = fields.get( 0 );
                        }

                        return displayTimings( context.getSource(), sort, fields );
                    } ) ) )
        );
    }

    private static TextComponent linkComputer( ServerCommandSource source, ServerComputer serverComputer, int computerId )
    {
        TextComponent out = new StringTextComponent( "" );

        // Append the computer instance
        if( serverComputer == null )
        {
            out.append( text( "?" ) );
        }
        else
        {
            out.append( link(
                text( Integer.toString( serverComputer.getInstanceID() ) ),
                "/computercraft dump " + serverComputer.getInstanceID(),
                "View more info about this computer"
            ) );
        }

        // And ID
        out.append( " (id " + computerId + ")" );

        // And, if we're a player, some useful links
        if( serverComputer != null && UserLevel.OP.test( source ) && isPlayer( source ) )
        {
            out
                .append( " " )
                .append( link(
                    text( "\u261b" ),
                    "/computercraft tp " + serverComputer.getInstanceID(),
                    "Teleport to this computer"
                ) )
                .append( " " )
                .append( link(
                    text( "\u20e2" ),
                    "/computercraft view " + serverComputer.getInstanceID(),
                    "View this computer"
                ) );
        }

        return out;
    }

    private static TextComponent linkPosition( ServerCommandSource context, ServerComputer computer )
    {
        if( UserLevel.OP.test( context ) )
        {
            return link(
                position( computer.getPosition() ),
                "/computercraft tp " + computer.getInstanceID(),
                "Teleport to this computer"
            );
        }
        else
        {
            return position( computer.getPosition() );
        }
    }

    @Nonnull
    private static TrackingContext getTimingContext( ServerCommandSource source )
    {
        Entity entity = source.getEntity();
        return entity instanceof PlayerEntity ? Tracking.getContext( entity.getUuid() ) : Tracking.getContext( SYSTEM_UUID );
    }

    private static final List<TrackingField> DEFAULT_FIELDS = Arrays.asList( TrackingField.TASKS, TrackingField.TOTAL_TIME, TrackingField.AVERAGE_TIME, TrackingField.MAX_TIME );

    private static int displayTimings( ServerCommandSource source, TrackingField sortField, List<TrackingField> fields ) throws CommandSyntaxException
    {
        return displayTimings( source, getTimingContext( source ).getTimings(), sortField, fields );
    }

    private static int displayTimings( ServerCommandSource source, @Nonnull List<ComputerTracker> timings, @Nonnull TrackingField sortField, @Nonnull List<TrackingField> fields ) throws CommandSyntaxException
    {
        if( timings.isEmpty() ) throw NO_TIMINGS_EXCEPTION.create();

        Map<Computer, ServerComputer> lookup = new HashMap<>();
        int maxId = 0, maxInstance = 0;
        for( ServerComputer server : ComputerCraft.serverComputerRegistry.getComputers() )
        {
            lookup.put( server.getComputer(), server );

            if( server.getInstanceID() > maxInstance ) maxInstance = server.getInstanceID();
            if( server.getID() > maxId ) maxId = server.getID();
        }

        timings.sort( Comparator.<ComputerTracker, Long>comparing( x -> x.get( sortField ) ).reversed() );

        String[] headers = new String[1 + fields.size()];
        headers[0] = "Computer";
        for( int i = 0; i < fields.size(); i++ ) headers[i + 1] = fields.get( i ).displayName();
        TableBuilder table = new TableBuilder( TRACK_ID, headers );

        for( ComputerTracker entry : timings )
        {
            Computer computer = entry.getComputer();
            ServerComputer serverComputer = computer == null ? null : lookup.get( computer );

            TextComponent computerComponent = linkComputer( source, serverComputer, entry.getComputerId() );

            TextComponent[] row = new TextComponent[1 + fields.size()];
            row[0] = computerComponent;
            for( int i = 0; i < fields.size(); i++ ) row[i + 1] = text( entry.getFormatted( fields.get( i ) ) );
            table.row( row );
        }

        table.display( source );
        return timings.size();
    }
}
