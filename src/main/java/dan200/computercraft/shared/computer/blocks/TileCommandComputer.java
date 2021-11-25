/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.apis.CommandAPI;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TileCommandComputer extends TileComputer
{
    public class CommandReceiver implements ICommandSource
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
        public void sendMessage( @Nonnull ITextComponent textComponent, @Nonnull UUID id )
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

    public TileCommandComputer( ComputerFamily family, TileEntityType<? extends TileCommandComputer> type )
    {
        super( family, type );
        receiver = new CommandReceiver();
    }

    public CommandReceiver getReceiver()
    {
        return receiver;
    }

    public CommandSource getSource()
    {
        ServerComputer computer = getServerComputer();
        String name = "@";
        if( computer != null )
        {
            String label = computer.getLabel();
            if( label != null ) name = label;
        }

        return new CommandSource( receiver,
            new Vector3d( worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5 ), Vector2f.ZERO,
            (ServerWorld) getLevel(), 2,
            name, new StringTextComponent( name ),
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
    public boolean isUsable( PlayerEntity player, boolean ignoreRange )
    {
        return isUsable( player ) && super.isUsable( player, ignoreRange );
    }

    public static boolean isUsable( PlayerEntity player )
    {
        MinecraftServer server = player.getServer();
        if( server == null || !server.isCommandBlockEnabled() )
        {
            player.displayClientMessage( new TranslationTextComponent( "advMode.notEnabled" ), true );
            return false;
        }
        else if( ComputerCraft.commandRequireCreative ? !player.canUseGameMasterBlocks() : !server.getPlayerList().isOp( player.getGameProfile() ) )
        {
            player.displayClientMessage( new TranslationTextComponent( "advMode.notAllowed" ), true );
            return false;
        }

        return true;
    }
}
