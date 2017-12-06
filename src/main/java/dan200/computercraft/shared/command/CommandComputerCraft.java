package dan200.computercraft.shared.command;

import com.google.common.collect.Lists;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.command.framework.*;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

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
            "Display the status of all computers or specific information about one computer. You can either specify the computer's instance " +
                "id (e.g. 123) or computer id (e.g #123)."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                if( arguments.size() == 0 )
                {
                    TextTable table = new TextTable( "Instance", "Id", "On", "Position" );

                    int max = 50;
                    for( ServerComputer computer : ComputerCraft.serverComputerRegistry.getComputers() )
                    {
                        table.addRow(
                            linkComputer( computer ),
                            text( Integer.toString( computer.getID() ) ),
                            bool( computer.isOn() ),
                            linkPosition( context, computer )
                        );

                        if( max-- < 0 ) break;
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
                            table.addRow( header( "Peripheral " + Computer.s_sideNames[ i ] ), text( peripheral.getType() ) );
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
                    : Collections.<String>emptyList();
            }
        } );

        root.register( new SubCommandBase(
            "shutdown", "[ids...]", "Shutdown computers remotely.", UserLevel.OWNER_OP,
            "Shutdown the listed computers or all if none are specified. You can either specify the computer's instance " +
                "id (e.g. 123) or computer id (e.g #123)."
        )
        {
            @Override
            public void execute( @Nonnull CommandContext context, @Nonnull List<String> arguments ) throws CommandException
            {
                List<ServerComputer> computers = Lists.newArrayList();
                if( arguments.size() > 0 )
                {
                    for( String arg : arguments )
                    {
                        computers.add( ComputerSelector.getComputer( arg ) );
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
                    ? Collections.<String>emptyList()
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
                    : Collections.<String>emptyList();
            }
        } );

        root.register(new SubCommandBase(
            "view", "<id>", "View the terminal of a computer.", UserLevel.OP,
            "Open the terminal of a computer, allowing remote control of a computer. This does not provide access to " +
                "turtle's inventories. You can either specify the computer's instance id (e.g. 123) or computer id (e.g #123)."
        ) {
            @Override
            public void execute(@Nonnull CommandContext context, @Nonnull List<String> arguments) throws CommandException {
                if (arguments.size() != 1) throw new CommandException(context.getFullUsage());

                ICommandSender sender = context.getSender();
                if (!(sender instanceof EntityPlayerMP)) {
                    throw new CommandException("Cannot open terminal for non-player");
                }

                ServerComputer computer = ComputerSelector.getComputer(arguments.get(0));
                ComputerCraft.openComputerGUI( (EntityPlayerMP) sender, computer );
            }

            @Nonnull
            @Override
            public List<String> getCompletion(@Nonnull CommandContext context, @Nonnull List<String> arguments) {
                return arguments.size() == 1
                    ? ComputerSelector.completeComputer( arguments.get( 0 ) )
                    : Collections.emptyList();
            }
        });


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
}
