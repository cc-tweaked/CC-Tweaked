/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TileCommandComputer extends TileComputer
{
    public class CommandReceiver implements CommandSource
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
        public void sendMessage( @Nonnull Component textComponent, @Nonnull UUID id )
        {
            output.put( output.size() + 1, textComponent.getString() );
        }

        @Override
        public boolean acceptsSuccess()
        {
            return true;
        }

        @Override
        public boolean acceptsFailure()
        {
            return true;
        }

        @Override
        public boolean shouldInformAdmins()
        {
            return getLevel().getGameRules().getBoolean( GameRules.RULE_COMMANDBLOCKOUTPUT );
        }
    }

    private final CommandReceiver receiver;

    public TileCommandComputer( BlockEntityType<? extends TileComputer> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state, ComputerFamily.COMMAND );
        receiver = new CommandReceiver();
    }

    public CommandReceiver getReceiver()
    {
        return receiver;
    }

    public CommandSourceStack getSource()
    {
        ServerComputer computer = getServerComputer();
        String name = "@";
        if( computer != null )
        {
            String label = computer.getLabel();
            if( label != null ) name = label;
        }

        return new CommandSourceStack( receiver,
            new Vec3( worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5 ), Vec2.ZERO,
            (ServerLevel) getLevel(), 2,
            name, new TextComponent( name ),
            getLevel().getServer(), null
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
    public boolean isUsable( Player player, boolean ignoreRange )
    {
        return isUsable( player ) && super.isUsable( player, ignoreRange );
    }

    public static boolean isUsable( Player player )
    {
        MinecraftServer server = player.getServer();
        if( server == null || !server.isCommandBlockEnabled() )
        {
            player.displayClientMessage( new TranslatableComponent( "advMode.notEnabled" ), true );
            return false;
        }
        else if( ComputerCraft.commandRequireCreative ? !player.canUseGameMasterBlocks() : !server.getPlayerList().isOp( player.getGameProfile() ) )
        {
            player.displayClientMessage( new TranslatableComponent( "advMode.notAllowed" ), true );
            return false;
        }

        return true;
    }
}
