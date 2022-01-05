/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.ComputerCraftAPIImpl;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ComputerDataClientMessage;
import dan200.computercraft.shared.network.client.ComputerDeletedClientMessage;
import dan200.computercraft.shared.network.client.ComputerTerminalClientMessage;
import me.shedaniel.cloth.api.utils.v1.GameInstanceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public class ServerComputer extends ServerTerminal implements IComputer, IComputerEnvironment
{
    private final int instanceID;
    private final ComputerFamily family;
    private final Computer computer;
    private Level world;
    private BlockPos position;
    private CompoundTag userData;
    private boolean changed;

    private boolean changedLastFrame;
    private int ticksSincePing;

    public ServerComputer( Level world, int computerID, String label, int instanceID, ComputerFamily family, int terminalWidth, int terminalHeight )
    {
        super( family != ComputerFamily.NORMAL, terminalWidth, terminalHeight );
        this.instanceID = instanceID;

        this.world = world;
        position = null;

        this.family = family;
        computer = new Computer( this, getTerminal(), computerID );
        computer.setLabel( label );
        userData = null;
        changed = false;

        changedLastFrame = false;
        ticksSincePing = 0;
    }

    public ComputerFamily getFamily()
    {
        return family;
    }

    public Level getWorld()
    {
        return world;
    }

    public void setWorld( Level world )
    {
        this.world = world;
    }

    public BlockPos getPosition()
    {
        return position;
    }

    public void setPosition( BlockPos pos )
    {
        position = new BlockPos( pos );
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return computer.getAPIEnvironment();
    }

    public Computer getComputer()
    {
        return computer;
    }

    @Override
    public void update()
    {
        super.update();
        computer.tick();

        changedLastFrame = computer.pollAndResetChanged() || changed;
        changed = false;

        ticksSincePing++;
    }

    public void keepAlive()
    {
        ticksSincePing = 0;
    }

    public boolean hasTimedOut()
    {
        return ticksSincePing > 100;
    }

    public void unload()
    {
        computer.unload();
    }

    public CompoundTag getUserData()
    {
        if( userData == null )
        {
            userData = new CompoundTag();
        }
        return userData;
    }

    public void updateUserData()
    {
        changed = true;
    }

    public void broadcastState( boolean force )
    {
        if( hasOutputChanged() || force )
        {
            // Send computer state to all clients
            MinecraftServer server = GameInstanceUtils.getServer();
            if( server != null )
            {
                NetworkHandler.sendToAllPlayers( server, createComputerPacket() );
            }
        }

        if( hasTerminalChanged() || force )
        {
            MinecraftServer server = GameInstanceUtils.getServer();
            if( server != null )
            {
                // Send terminal state to clients who are currently interacting with the computer.

                NetworkMessage packet = null;
                for( Player player : server.getPlayerList()
                    .getPlayers() )
                {
                    if( isInteracting( player ) )
                    {
                        if( packet == null )
                        {
                            packet = createTerminalPacket();
                        }
                        NetworkHandler.sendToPlayer( player, packet );
                    }
                }
            }
        }
    }

    public boolean hasOutputChanged()
    {
        return changedLastFrame;
    }

    private NetworkMessage createComputerPacket()
    {
        return new ComputerDataClientMessage( this );
    }

    protected boolean isInteracting( Player player )
    {
        return getContainer( player ) != null;
    }

    protected NetworkMessage createTerminalPacket()
    {
        return new ComputerTerminalClientMessage( getInstanceID(), write() );
    }

    @Nullable
    public IContainerComputer getContainer( Player player )
    {
        if( player == null )
        {
            return null;
        }

        AbstractContainerMenu container = player.containerMenu;
        if( !(container instanceof IContainerComputer) )
        {
            return null;
        }

        IContainerComputer computerContainer = (IContainerComputer) container;
        return computerContainer.getComputer() != this ? null : computerContainer;
    }

    @Override
    public int getInstanceID()
    {
        return instanceID;
    }

    @Override
    public void turnOn()
    {
        // Turn on
        computer.turnOn();
    }

    // IComputer

    @Override
    public void shutdown()
    {
        // Shutdown
        computer.shutdown();
    }

    @Override
    public void reboot()
    {
        // Reboot
        computer.reboot();
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Queue event
        computer.queueEvent( event, arguments );
    }

    @Override
    public boolean isOn()
    {
        return computer.isOn();
    }

    @Override
    public boolean isCursorDisplayed()
    {
        return computer.isOn() && computer.isBlinking();
    }

    public void sendComputerState( Player player )
    {
        // Send state to client
        NetworkHandler.sendToPlayer( player, createComputerPacket() );
    }

    public void sendTerminalState( Player player )
    {
        // Send terminal state to client
        NetworkHandler.sendToPlayer( player, createTerminalPacket() );
    }

    public void broadcastDelete()
    {
        // Send deletion to client
        MinecraftServer server = GameInstanceUtils.getServer();
        if( server != null )
        {
            NetworkHandler.sendToAllPlayers( server, new ComputerDeletedClientMessage( getInstanceID() ) );
        }
    }

    public int getID()
    {
        return computer.getID();
    }

    public void setID( int id )
    {
        computer.setID( id );
    }

    public String getLabel()
    {
        return computer.getLabel();
    }

    public void setLabel( String label )
    {
        computer.setLabel( label );
    }

    public int getRedstoneOutput( ComputerSide side )
    {
        return computer.getEnvironment()
            .getExternalRedstoneOutput( side );
    }

    public void setRedstoneInput( ComputerSide side, int level )
    {
        computer.getEnvironment()
            .setRedstoneInput( side, level );
    }

    public int getBundledRedstoneOutput( ComputerSide side )
    {
        return computer.getEnvironment()
            .getExternalBundledRedstoneOutput( side );
    }

    public void setBundledRedstoneInput( ComputerSide side, int combination )
    {
        computer.getEnvironment()
            .setBundledRedstoneInput( side, combination );
    }

    public void addAPI( ILuaAPI api )
    {
        computer.addApi( api );
    }

    // IComputerEnvironment implementation

    public void setPeripheral( ComputerSide side, IPeripheral peripheral )
    {
        computer.getEnvironment()
            .setPeripheral( side, peripheral );
    }

    public IPeripheral getPeripheral( ComputerSide side )
    {
        return computer.getEnvironment()
            .getPeripheral( side );
    }

    @Override
    public int getDay()
    {
        return (int) ((world.getDayTime() + 6000) / 24000) + 1;
    }

    @Override
    public double getTimeOfDay()
    {
        return (world.getDayTime() + 6000) % 24000 / 1000.0;
    }

    @Override
    public long getComputerSpaceLimit()
    {
        return ComputerCraft.computerSpaceLimit;
    }

    @Nonnull
    @Override
    public String getHostString()
    {
        return String.format( "ComputerCraft %s (Minecraft %s)", ComputerCraftAPI.getInstalledVersion(), "1.16.4" );
    }

    @Nonnull
    @Override
    public String getUserAgent()
    {
        return ComputerCraft.MOD_ID + "/" + ComputerCraftAPI.getInstalledVersion();
    }

    @Override
    public int assignNewID()
    {
        return ComputerCraftAPI.createUniqueNumberedSaveDir( world, "computer" );
    }

    @Override
    public IWritableMount createSaveDirMount( String subPath, long capacity )
    {
        return ComputerCraftAPI.createSaveDirMount( world, subPath, capacity );
    }

    @Override
    public IMount createResourceMount( String domain, String subPath )
    {
        return ComputerCraftAPI.createResourceMount( domain, subPath );
    }

    @Override
    public InputStream createResourceFile( String domain, String subPath )
    {
        return ComputerCraftAPIImpl.getResourceFile( domain, subPath );
    }
}
