package dan200.computercraft;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.ArrayUtils;

public class ComputerCommand extends CommandBase {

    @Override
    public String getName() {
        return "computer";
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "computer <id> <value1> [value2]...";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if( args.length < 2 ){
            throw new CommandException( "Usage: /computer <id> <value1> [value2]..." );
        }
        try {
            ServerComputer computer = ComputerCraft.serverComputerRegistry.lookup(Integer.valueOf(args[0]));
            if( computer != null && computer.getFamily() == ComputerFamily.Command ){
                computer.queueEvent( "computer_command", ArrayUtils.remove( args, 0 ) );
            }else{
                throw new CommandException( "Computer #" + args[0] + " is not a Command Computer" );
            }
        }catch( NumberFormatException e ){
            throw new CommandException( "Invalid ID" );
        }
    }

    @Override
    public int getRequiredPermissionLevel(){
        return 0;
    }
}
