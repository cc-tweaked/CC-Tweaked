package dan200.computercraft.shared.command;

import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.tracking.ComputerTracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingContext;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.command.framework.*;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
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

import static dan200.computercraft.shared.command.framework.ChatHelpers.*;

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
        CommandRoot root = new CommandRoot(
            "computercraft", "Various commands for controlling computers.",
            "The /computercraft command provides various debugging and administrator tools for controlling and " +
                "interacting with computers."
        );

        root.register( new SubCommandBase(
            "dump", "[id]", "Display the status of computers.", UserLevel.OWNER_OP,
            "Display the status of all computers or specific information about one computer. You can specify the " +
                "computer's instance id (e.g. 123), computer id (e.g #123) or label (e.g. \"@My Computer\")."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() == 0 )
                {
                    TextTable table = new TextTable( DUMP_LIST_ID, "Computer", "On", "Position" );

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
                        table.addRow(
                            linkComputer( context, computer, computer.getID() ),
                            bool( computer.isOn() ),
                            linkPosition( context, computer )
                        );
                    }

                    table.displayTo( context.getSender() );
                }
                else if( arguments.size() == 1 )
                {
                    ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );

                    TextTable table = new TextTable( DUMP_SINGLE_ID );
                    table.addRow( header( "Instance" ), text( Integer.toString( computer.getInstanceID() ) ) );
                    table.addRow( header( "Id" ), text( Integer.toString( computer.getID() ) ) );
                    table.addRow( header( "Label" ), text( computer.getLabel() ) );
                    table.addRow( header( "On" ), bool( computer.isOn() ) );
                    table.addRow( header( "Position" ), linkPosition( context, computer ) );
                    table.addRow( header( "Family" ), text( computer.getFamily().toString() ) );

                    for( int i = 0; i < 6; i++ )
                    {
                        IPeripheral peripheral = computer.getPeripheral( i );
                        if( peripheral != null )
                        {
                            table.addRow( header( "Peripheral " + Computer.s_sideNames[i] ), text( peripheral.getType() ) );
                        }
                    }

                    table.displayTo( context.getSender() );
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

        root.register( new SubCommandBase(
            "shutdown", "[ids...]", "Shutdown computers remotely.", UserLevel.OWNER_OP,
            "Shutdown the listed computers or all if none are specified. You can specify the computer's instance id " +
                "(e.g. 123), computer id (e.g #123) or label (e.g. \"@My Computer\")."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                withComputers( arguments, computers -> {
                    int shutdown = 0;
                    for( ServerComputer computer : computers )
                    {
                        if( computer.isOn() ) shutdown++;
                        computer.unload();
                    }
                    context.getSender().sendMessage( text( "Shutdown " + shutdown + " / " + computers.size() + " computers" ) );
                } );
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.size() == 0
                    ? Collections.emptyList()
                    : ComputerSelector.completeComputer( arguments.get( arguments.size() - 1 ) );
            }
        } );

        root.register( new SubCommandBase(
            "turn-on", "ids...", "Turn computers on remotely.", UserLevel.OWNER_OP,
            "Turn on the listed computers. You can specify the computer's instance id (e.g. 123), computer id (e.g #123) " +
                "or label (e.g. \"@My Computer\")."
        )
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
                    context.getSender().sendMessage( text( "Turned on " + on + " / " + computers.size() + " computers" ) );
                } );
            }

            @Nonnull
            @Override
            public List<String> getCompletion( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                return arguments.size() == 0
                    ? Collections.emptyList()
                    : ComputerSelector.completeComputer( arguments.get( arguments.size() - 1 ) );
            }
        } );

        root.register( new SubCommandBase(
            "tp", "<id>", "Teleport to a specific computer.", UserLevel.OP,
            "Teleport to the location of a computer. You can either specify the computer's instance " +
                "id (e.g. 123) or computer id (e.g #123)."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() != 1 ) throw new CommandException( context.getFullUsage() );

                ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );
                World world = computer.getWorld();
                BlockPos pos = computer.getPosition();

                if( world == null || pos == null ) throw new CommandException( "Cannot locate computer in world" );

                ICommandSender sender = context.getSender();
                if( !(sender instanceof Entity) ) throw new CommandException( "Sender is not an entity" );

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

        root.register( new SubCommandBase(
            "view", "<id>", "View the terminal of a computer.", UserLevel.OP,
            "Open the terminal of a computer, allowing remote control of a computer. This does not provide access to " +
                "turtle's inventories. You can either specify the computer's instance id (e.g. 123) or computer id (e.g #123)."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() != 1 ) throw new CommandException( context.getFullUsage() );

                ICommandSender sender = context.getSender();
                if( !(sender instanceof EntityPlayerMP) )
                {
                    throw new CommandException( "Cannot open terminal for non-player" );
                }

                ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );
                ComputerCraft.openComputerGUI( (EntityPlayerMP) sender, computer );
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

        CommandRoot track = new CommandRoot( "track", "Track execution times for computers.",
            "Track how long computers execute for, as well as how many events they handle. This presents information in " +
                "a similar way to /forge track and can be useful for diagnosing lag." );
        root.register( track );

        track.register( new SubCommandBase(
            "start", "Start tracking all computers", UserLevel.OWNER_OP,
            "Start tracking all computers' execution times and event counts. This will discard the results of previous runs."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                getTimingContext( context ).start();

                String stopCommand = "/" + context.parent().getFullPath() + " stop";
                context.getSender().sendMessage( list(
                    text( "Run " ),
                    link( text( stopCommand ), stopCommand, "Click to stop tracking" ),
                    text( " to stop tracking and view the results" )
                ) );
            }
        } );

        track.register( new SubCommandBase(
            "stop", "Stop tracking all computers", UserLevel.OWNER_OP,
            "Stop tracking all computers' events and execution times"
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                TrackingContext timings = getTimingContext( context );
                if( !timings.stop() ) throw new CommandException( "Tracking not enabled" );
                displayTimings( context, timings.getImmutableTimings(), TrackingField.AVERAGE_TIME );
            }
        } );

        track.register( new SubCommandBase(
            "dump", "[kind]", "Dump the latest track results", UserLevel.OWNER_OP,
            "Dump the latest results of computer tracking."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                TrackingField field = TrackingField.AVERAGE_TIME;
                if( arguments.size() >= 1 )
                {
                    field = TrackingField.fields().get( arguments.get( 0 ) );
                    if( field == null ) throw new CommandException( "Unknown field '" + arguments.get( 0 ) + "'" );
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
        } );

        root.register( new SubCommandBase(
            "reload", "Reload the ComputerCraft config file", UserLevel.OWNER_OP,
            "Reload the ComputerCraft config file"
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments )
            {
                ComputerCraft.loadConfig();
                ComputerCraft.syncConfig();
                context.getSender().sendMessage( new TextComponentString( "Reloaded config" ) );
            }
        } );

        root.register( new SubCommandBase(
            "queue", "<id> [args...]", "Send a computer_command event to a command computer", UserLevel.ANYONE,
            "Send a computer_command event to a command computer, passing through the additional arguments. " +
                "This is mostly designed for map makers, acting as a more computer-friendly version of /trigger. Any " +
                "player can run the command, which would most likely be done through a text component's click event."
        )
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
                    throw new CommandException( "Could not find any command computers matching " + selector );
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
                "View more info about this computer"
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
                    "Teleport to this computer"
                ) )
                .appendText( " " )
                .appendSibling( link(
                    text( "\u20e2" ),
                    "/computercraft view " + serverComputer.getInstanceID(),
                    "View this computer"
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
                "Teleport to this computer"
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
        if( timings.isEmpty() ) throw new CommandException( "No timings available" );

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


        TextTable table = defaultLayout
            ? new TextTable( TRACK_ID, "Computer", "Tasks", "Total", "Average", "Maximum" )
            : new TextTable( TRACK_ID, "Computer", field.displayName() );

        for( ComputerTracker entry : timings )
        {
            Computer computer = entry.getComputer();
            ServerComputer serverComputer = computer == null ? null : lookup.get( computer );

            ITextComponent computerComponent = linkComputer( context, serverComputer, entry.getComputerId() );

            if( defaultLayout )
            {
                table.addRow(
                    computerComponent,
                    text( entry.getFormatted( TrackingField.TASKS ) ),
                    text( entry.getFormatted( TrackingField.TOTAL_TIME ) ),
                    text( entry.getFormatted( TrackingField.AVERAGE_TIME ) ),
                    text( entry.getFormatted( TrackingField.MAX_TIME ) )
                );
            }
            else
            {
                table.addRow( computerComponent, text( entry.getFormatted( field ) ) );
            }
        }

        table.displayTo( context.getSender() );
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
            throw new CommandException( "Could not find computers matching " + String.join( ", ", failed ) );
        }
    }
}
