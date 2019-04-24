/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class TileCommandComputer extends TileComputer
{
    public static final NamedBlockEntityType<TileCommandComputer> FACTORY = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "command_computer" ),
        f -> new TileCommandComputer( ComputerFamily.Command, f )
    );

    public class CommandReceiver implements CommandOutput
    {
        private final Map<Integer, String> output = new HashMap<>();

        public void clearOutput()
        {
            output.clear();
        }

        public Map<Integer, String> getOutput()
        {
            return output;
        }

        public Map<Integer, String> copyOutput()
        {
            return new HashMap<>( output );
        }

        @Override
        public void sendMessage( TextComponent textComponent )
        {
            output.put( output.size() + 1, textComponent.getText() );
        }

        @Override
        public boolean sendCommandFeedback()
        {
            return getWorld().getGameRules().getBoolean( "sendCommandFeedback" );
        }

        @Override
        public boolean shouldTrackOutput()
        {
            return true;
        }

        @Override
        public boolean shouldBroadcastConsoleToOps()
        {
            return getWorld().getGameRules().getBoolean( "commandBlockOutput" );
        }
    }

    private final CommandReceiver receiver;

    public TileCommandComputer( ComputerFamily family, BlockEntityType<? extends TileCommandComputer> type )
    {
        super( family, type );
        receiver = new CommandReceiver();
    }

    public CommandReceiver getReceiver()
    {
        return receiver;
    }

    public ServerCommandSource getSource()
    {
        ServerComputer computer = getServerComputer();
        String name = "@";
        if( computer != null )
        {
            String label = computer.getLabel();
            if( label != null ) name = label;
        }

        return new ServerCommandSource( receiver,
            new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ), Vec2f.ZERO,
            (ServerWorld) getWorld(), 2,
            name, new StringTextComponent( name ),
            getWorld().getServer(), null
        );
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ServerComputer computer = super.createComputer( instanceID, id );
        computer.addAPI( new CommandAPI( this ) );
        return computer;
    }

    @Override
    public boolean isUsable( PlayerEntity player, boolean ignoreRange )
    {
        MinecraftServer server = player.getServer();
        if( server == null || !server.areCommandBlocksEnabled() )
        {
            player.addChatMessage( new TranslatableTextComponent( "advMode.notEnabled" ), true );
            return false;
        }
        else if( !player.isCreativeLevelTwoOp() )
        {
            player.addChatMessage( new TranslatableTextComponent( "advMode.notAllowed" ), true );
            return false;
        }
        else
        {
            return super.isUsable( player, ignoreRange );
        }
    }
}
