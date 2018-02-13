package dan200.computercraft.shared.command;

import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerTimeTracker;
import dan200.computercraft.shared.command.framework.*;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nonnull;
import java.util.*;

import static dan200.computercraft.shared.command.framework.ChatHelpers.*;

public final class CommandComputerCraft extends CommandDelegate
{
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
                    TextTable table = new TextTable( "Instance", "Id", "On", "Position" );

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
                            linkComputer( computer ),
                            text( Integer.toString( computer.getID() ) ),
                            bool( computer.isOn() ),
                            linkPosition( context, computer )
                        );
                    }

                    table.displayTo( context.getSender() );
                }
                else if( arguments.size() == 1 )
                {
                    ServerComputer computer = ComputerSelector.getComputer( arguments.get( 0 ) );

                    TextTable table = new TextTable();
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
                Set<ServerComputer> computers = Sets.newHashSet();
                if( arguments.size() > 0 )
                {
                    for( String arg : arguments )
                    {
                        computers.addAll( ComputerSelector.getComputers( arg ) );
                    }
                }
                else
                {
                    computers.addAll( ComputerCraft.serverComputerRegistry.getComputers() );
                }

                int shutdown = 0;
                for( ServerComputer computer : computers )
                {
                    if( computer.isOn() ) shutdown++;
                    computer.unload();
                }
                context.getSender().sendMessage( text( "Shutdown " + shutdown + " / " + computers.size() + " computers" ) );
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
                Set<ServerComputer> computers = Sets.newHashSet();
                if( arguments.size() > 0 )
                {
                    for( String arg : arguments )
                    {
                        computers.addAll( ComputerSelector.getComputers( arg ) );
                    }
                }
                else
                {
                    computers.addAll( ComputerCraft.serverComputerRegistry.getComputers() );
                }

                int on = 0;
                for( ServerComputer computer : computers )
                {
                    if( !computer.isOn() ) on++;
                    computer.turnOn();
                }
                context.getSender().sendMessage( text( "Turned on " + on + " / " + computers.size() + " computers" ) );
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
                ComputerTimeTracker.start();

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
                if( !ComputerTimeTracker.stop() ) throw new CommandException( "Tracking not enabled" );
                displayTimings( context );
            }
        } );

        track.register( new SubCommandBase(
            "dump", "Dump the latest track results", UserLevel.OWNER_OP,
            "Dump the latest results of computer tracking."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                displayTimings( context );
            }
        } );


        return root;
    }

    private static ITextComponent linkComputer( ServerComputer computer )
    {
        return link(
            text( Integer.toString( computer.getInstanceID() ) ),
            "/computercraft dump " + computer.getInstanceID(),
            "View more info about this computer"
        );
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

    private static void displayTimings( CommandContext context ) throws CommandException
    {
        List<ComputerTimeTracker.Timings> timings = ComputerTimeTracker.getTimings();
        if( timings.isEmpty() ) throw new CommandException( "No timings available" );

        timings.sort( Comparator.comparing( ComputerTimeTracker.Timings::getAverage ).reversed() );
        TextTable table = new TextTable( "Computer", "Tasks", "Total", "Average", "Maximum" );

        Map<Computer, ServerComputer> lookup = new HashMap<>();
        int maxId = 0, maxInstance = 0;
        for( ServerComputer server : ComputerCraft.serverComputerRegistry.getComputers() )
        {
            lookup.put( server.getComputer(), server );

            if( server.getInstanceID() > maxInstance ) maxInstance = server.getInstanceID();
            if( server.getID() > maxId ) maxId = server.getID();
        }

        ICommandSender sender = context.getSender();
        boolean isPlayer = sender instanceof EntityPlayerMP && !(sender instanceof FakePlayer);

        for( ComputerTimeTracker.Timings entry : timings )
        {
            Computer computer = entry.getComputer();
            ServerComputer serverComputer = computer == null ? null : lookup.get( computer );

            ITextComponent computerComponent = new TextComponentString( "" )
                .appendSibling( serverComputer == null ? text( "?" ) : linkComputer( serverComputer ) )
                .appendText( " (id " + entry.getComputerId() + ")" );

            if( serverComputer != null && UserLevel.OP.canExecute( context ) && isPlayer )
            {
                computerComponent
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

            table.addRow(
                computerComponent,
                formatted( "%4d", entry.getTasks() ),
                text( String.format( "%7.1f", entry.getTotalTime() / 1e6 ) + "ms" ),
                text( String.format( "%4.1f", entry.getAverage() / 1e6 ) + "ms" ),
                text( String.format( "%5.1f", entry.getMaxTime() / 1e6 ) + "ms" )
            );
        }

        table.displayTo( context.getSender() );
    }
}
