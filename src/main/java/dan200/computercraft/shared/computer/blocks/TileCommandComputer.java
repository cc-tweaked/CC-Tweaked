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
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class TileCommandComputer extends TileComputer
{
    public static final NamedBlockEntityType<TileCommandComputer> FACTORY = NamedBlockEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "command_computer" ),
        f -> new TileCommandComputer( ComputerFamily.Command, f )
    );

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
        public void sendMessage( @Nonnull ITextComponent textComponent )
        {
            output.put( output.size() + 1, textComponent.getString() );
        }

        @Override
        public boolean shouldReceiveFeedback()
        {
            return getWorld().getGameRules().getBoolean( "sendCommandFeedback" );
        }

        @Override
        public boolean shouldReceiveErrors()
        {
            return true;
        }

        @Override
        public boolean allowLogging()
        {
            return getWorld().getGameRules().getBoolean( "commandBlockOutput" );
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
            new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 ), Vec2f.ZERO,
            (WorldServer) getWorld(), 2,
            name, new TextComponentString( name ),
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
    public boolean isUsable( EntityPlayer player, boolean ignoreRange )
    {
        MinecraftServer server = player.getServer();
        if( server == null || !server.isCommandBlockEnabled() )
        {
            player.sendStatusMessage( new TextComponentTranslation( "advMode.notEnabled" ), true );
            return false;
        }
        else if( !player.canUseCommandBlock() )
        {
            player.sendStatusMessage( new TextComponentTranslation( "advMode.notAllowed" ), true );
            return false;
        }
        else
        {
            return super.isUsable( player, ignoreRange );
        }
    }
}
