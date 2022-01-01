/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.tracking.ComputerTracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingContext;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.network.container.ViewComputerContainerData;
import dan200.computercraft.shared.util.IDAssigner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

import static dan200.computercraft.shared.command.CommandUtils.isPlayer;
import static dan200.computercraft.shared.command.Exceptions.*;
import static dan200.computercraft.shared.command.arguments.ComputerArgumentType.getComputerArgument;
import static dan200.computercraft.shared.command.arguments.ComputerArgumentType.oneComputer;
import static dan200.computercraft.shared.command.arguments.ComputersArgumentType.*;
import static dan200.computercraft.shared.command.arguments.TrackingFieldArgumentType.trackingField;
import static dan200.computercraft.shared.command.builder.CommandBuilder.args;
import static dan200.computercraft.shared.command.builder.CommandBuilder.command;
import static dan200.computercraft.shared.command.builder.HelpingArgumentBuilder.choice;
import static dan200.computercraft.shared.command.text.ChatHelpers.*;
import static net.minecraft.commands.Commands.literal;

public final class CommandComputerCraft
{
    public static final UUID SYSTEM_UUID = new UUID( 0, 0 );

    private static final int DUMP_LIST_ID = 5373952;
    private static final int DUMP_SINGLE_ID = 1844510720;
    private static final int TRACK_ID = 373882880;

    private CommandComputerCraft()
    {
    }

    public static void register( CommandDispatcher<CommandSourceStack> dispatcher )
    {
        dispatcher.register( choice( "computercraft" )
            .then( literal( "dump" )
                .requires( UserLevel.OWNER_OP )
                .executes( context -> {
                    TableBuilder table = new TableBuilder( DUMP_LIST_ID, "Computer", "On", "Position" );

                    CommandSourceStack source = context.getSource();
                    List<ServerComputer> computers = new ArrayList<>( ComputerCraft.serverComputerRegistry.getComputers() );

                    // Unless we're on a server, limit the number of rows we can send.
                    Level world = source.getLevel();
                    BlockPos pos = new BlockPos( source.getPosition() );

                    computers.sort( ( a, b ) -> {
                        if( a.getLevel() == b.getLevel() && a.getLevel() == world )
                        {
                            return Double.compare( a.getPosition().distSqr( pos ), b.getPosition().distSqr( pos ) );
                        }
                        else if( a.getLevel() == world )
                        {
                            return -1;
                        }
                        else if( b.getLevel() == world )
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

                        for( ComputerSide side : ComputerSide.values() )
                        {
                            IPeripheral peripheral = computer.getPeripheral( side );
                            if( peripheral != null )
                            {
                                table.row( header( "Peripheral " + side.getName() ), text( peripheral.getType() ) );
                            }
                        }

                        table.display( context.getSource() );
                        return 1;
                    } ) ) )

            .then( command( "shutdown" )
                .requires( UserLevel.OWNER_OP )
                .argManyValue( "computers", manyComputers(), s -> ComputerCraft.serverComputerRegistry.getComputers() )
                .executes( ( context, computers ) -> {
                    int shutdown = 0;
                    for( ServerComputer computer : unwrap( context.getSource(), computers ) )
                    {
                        if( computer.isOn() ) shutdown++;
                        computer.shutdown();
                    }
                    context.getSource().sendSuccess( translate( "commands.computercraft.shutdown.done", shutdown, computers.size() ), false );
                    return shutdown;
                } ) )

            .then( command( "turn-on" )
                .requires( UserLevel.OWNER_OP )
                .argManyValue( "computers", manyComputers(), s -> ComputerCraft.serverComputerRegistry.getComputers() )
                .executes( ( context, computers ) -> {
                    int on = 0;
                    for( ServerComputer computer : unwrap( context.getSource(), computers ) )
                    {
                        if( !computer.isOn() ) on++;
                        computer.turnOn();
                    }
                    context.getSource().sendSuccess( translate( "commands.computercraft.turn_on.done", on, computers.size() ), false );
                    return on;
                } ) )

            .then( command( "tp" )
                .requires( UserLevel.OP )
                .arg( "computer", oneComputer() )
                .executes( context -> {
                    ServerComputer computer = getComputerArgument( context, "computer" );
                    Level world = computer.getLevel();
                    BlockPos pos = computer.getPosition();

                    if( world == null || pos == null ) throw TP_NOT_THERE.create();

                    Entity entity = context.getSource().getEntityOrException();
                    if( !(entity instanceof ServerPlayer player) ) throw TP_NOT_PLAYER.create();

                    if( player.getCommandSenderWorld() == world )
                    {
                        player.connection.teleport(
                            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0,
                            EnumSet.noneOf( ClientboundPlayerPositionPacket.RelativeArgument.class )
                        );
                    }
                    else
                    {
                        player.teleportTo( (ServerLevel) world,
                            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0
                        );
                    }

                    return 1;
                } ) )

            .then( command( "queue" )
                .requires( UserLevel.ANYONE )
                .arg( "computer", manyComputers() )
                .argManyValue( "args", StringArgumentType.string(), Collections.emptyList() )
                .executes( ( ctx, args ) -> {
                    Collection<ServerComputer> computers = getComputersArgument( ctx, "computer" );
                    Object[] rest = args.toArray();

                    int queued = 0;
                    for( ServerComputer computer : computers )
                    {
                        if( computer.getFamily() == ComputerFamily.COMMAND && computer.isOn() )
                        {
                            computer.queueEvent( "computer_command", rest );
                            queued++;
                        }
                    }

                    return queued;
                } ) )

            .then( command( "view" )
                .requires( UserLevel.OP )
                .arg( "computer", oneComputer() )
                .executes( context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerComputer computer = getComputerArgument( context, "computer" );
                    new ViewComputerContainerData( computer ).open( player, new MenuProvider()
                    {
                        @Nonnull
                        @Override
                        public Component getDisplayName()
                        {
                            return new TranslatableComponent( "gui.computercraft.view_computer" );
                        }

                        @Nonnull
                        @Override
                        public AbstractContainerMenu createMenu( int id, @Nonnull Inventory player, @Nonnull Player entity )
                        {
                            return new ContainerViewComputer( id, player, computer );
                        }
                    } );
                    return 1;
                } ) )

            .then( choice( "track" )
                .then( command( "start" )
                    .requires( UserLevel.OWNER_OP )
                    .executes( context -> {
                        getTimingContext( context.getSource() ).start();

                        String stopCommand = "/computercraft track stop";
                        context.getSource().sendSuccess( translate( "commands.computercraft.track.start.stop",
                            link( text( stopCommand ), stopCommand, translate( "commands.computercraft.track.stop.action" ) ) ), false );
                        return 1;
                    } ) )

                .then( command( "stop" )
                    .requires( UserLevel.OWNER_OP )
                    .executes( context -> {
                        TrackingContext timings = getTimingContext( context.getSource() );
                        if( !timings.stop() ) throw NOT_TRACKING_EXCEPTION.create();
                        displayTimings( context.getSource(), timings.getImmutableTimings(), TrackingField.AVERAGE_TIME, DEFAULT_FIELDS );
                        return 1;
                    } ) )

                .then( command( "dump" )
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

    private static Component linkComputer( CommandSourceStack source, ServerComputer serverComputer, int computerId )
    {
        MutableComponent out = new TextComponent( "" );

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
                translate( "commands.computercraft.dump.action" )
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
                    translate( "commands.computercraft.tp.action" )
                ) )
                .append( " " )
                .append( link(
                    text( "\u20e2" ),
                    "/computercraft view " + serverComputer.getInstanceID(),
                    translate( "commands.computercraft.view.action" )
                ) );
        }

        if( UserLevel.OWNER.test( source ) && isPlayer( source ) )
        {
            Component linkPath = linkStorage( computerId );
            if( linkPath != null ) out.append( " " ).append( linkPath );
        }

        return out;
    }

    private static Component linkPosition( CommandSourceStack context, ServerComputer computer )
    {
        if( UserLevel.OP.test( context ) )
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

    private static Component linkStorage( int id )
    {
        File file = new File( IDAssigner.getDir(), "computer/" + id );
        if( !file.isDirectory() ) return null;

        return link(
            text( "\u270E" ),
            ClientCommands.OPEN_COMPUTER + id,
            translate( "commands.computercraft.dump.open_path" )
        );
    }

    @Nonnull
    private static TrackingContext getTimingContext( CommandSourceStack source )
    {
        Entity entity = source.getEntity();
        return entity instanceof Player ? Tracking.getContext( entity.getUUID() ) : Tracking.getContext( SYSTEM_UUID );
    }

    private static final List<TrackingField> DEFAULT_FIELDS = Arrays.asList( TrackingField.TASKS, TrackingField.TOTAL_TIME, TrackingField.AVERAGE_TIME, TrackingField.MAX_TIME );

    private static int displayTimings( CommandSourceStack source, TrackingField sortField, List<TrackingField> fields ) throws CommandSyntaxException
    {
        return displayTimings( source, getTimingContext( source ).getTimings(), sortField, fields );
    }

    private static int displayTimings( CommandSourceStack source, @Nonnull List<ComputerTracker> timings, @Nonnull TrackingField sortField, @Nonnull List<TrackingField> fields ) throws CommandSyntaxException
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

        Component[] headers = new Component[1 + fields.size()];
        headers[0] = translate( "commands.computercraft.track.dump.computer" );
        for( int i = 0; i < fields.size(); i++ ) headers[i + 1] = translate( fields.get( i ).translationKey() );
        TableBuilder table = new TableBuilder( TRACK_ID, headers );

        for( ComputerTracker entry : timings )
        {
            Computer computer = entry.getComputer();
            ServerComputer serverComputer = computer == null ? null : lookup.get( computer );

            Component computerComponent = linkComputer( source, serverComputer, entry.getComputerId() );

            Component[] row = new Component[1 + fields.size()];
            row[0] = computerComponent;
            for( int i = 0; i < fields.size(); i++ ) row[i + 1] = text( entry.getFormatted( fields.get( i ) ) );
            table.row( row );
        }

        table.display( source );
        return timings.size();
    }
}
