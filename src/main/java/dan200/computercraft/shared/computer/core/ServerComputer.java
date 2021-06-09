/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public class ServerComputer extends ServerTerminal implements IComputer, IComputerEnvironment
{
    private final int m_instanceID;
    private final ComputerFamily m_family;
    private final Computer m_computer;
    private World m_world;
    private BlockPos m_position;
    private CompoundTag m_userData;
    private boolean m_changed;

    private boolean m_changedLastFrame;
    private int m_ticksSincePing;

    public ServerComputer( World world, int computerID, String label, int instanceID, ComputerFamily family, int terminalWidth, int terminalHeight )
    {
        super( family != ComputerFamily.NORMAL, terminalWidth, terminalHeight );
        this.m_instanceID = instanceID;

        this.m_world = world;
        this.m_position = null;

        this.m_family = family;
        this.m_computer = new Computer( this, this.getTerminal(), computerID );
        this.m_computer.setLabel( label );
        this.m_userData = null;
        this.m_changed = false;

        this.m_changedLastFrame = false;
        this.m_ticksSincePing = 0;
    }

    public ComputerFamily getFamily()
    {
        return this.m_family;
    }

    public World getWorld()
    {
        return this.m_world;
    }

    public void setWorld( World world )
    {
        this.m_world = world;
    }

    public BlockPos getPosition()
    {
        return this.m_position;
    }

    public void setPosition( BlockPos pos )
    {
        this.m_position = new BlockPos( pos );
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return this.m_computer.getAPIEnvironment();
    }

    public Computer getComputer()
    {
        return this.m_computer;
    }

    @Override
    public void update()
    {
        super.update();
        this.m_computer.tick();

        this.m_changedLastFrame = this.m_computer.pollAndResetChanged() || this.m_changed;
        this.m_changed = false;

        this.m_ticksSincePing++;
    }

    public void keepAlive()
    {
        this.m_ticksSincePing = 0;
    }

    public boolean hasTimedOut()
    {
        return this.m_ticksSincePing > 100;
    }

    public void unload()
    {
        this.m_computer.unload();
    }

    public CompoundTag getUserData()
    {
        if( this.m_userData == null )
        {
            this.m_userData = new CompoundTag();
        }
        return this.m_userData;
    }

    public void updateUserData()
    {
        this.m_changed = true;
    }

    public void broadcastState( boolean force )
    {
        if( this.hasOutputChanged() || force )
        {
            // Send computer state to all clients
            MinecraftServer server = GameInstanceUtils.getServer();
            if( server != null )
            {
                NetworkHandler.sendToAllPlayers( server, this.createComputerPacket() );
            }
        }

        if( this.hasTerminalChanged() || force )
        {
            MinecraftServer server = GameInstanceUtils.getServer();
            if( server != null )
            {
                // Send terminal state to clients who are currently interacting with the computer.

                NetworkMessage packet = null;
                for( PlayerEntity player : server.getPlayerManager()
                    .getPlayerList() )
                {
                    if( this.isInteracting( player ) )
                    {
                        if( packet == null )
                        {
                            packet = this.createTerminalPacket();
                        }
                        NetworkHandler.sendToPlayer( player, packet );
                    }
                }
            }
        }
    }

    public boolean hasOutputChanged()
    {
        return this.m_changedLastFrame;
    }

    private NetworkMessage createComputerPacket()
    {
        return new ComputerDataClientMessage( this );
    }

    protected boolean isInteracting( PlayerEntity player )
    {
        return this.getContainer( player ) != null;
    }

    protected NetworkMessage createTerminalPacket()
    {
        return new ComputerTerminalClientMessage( this.getInstanceID(), this.write() );
    }

    @Nullable
    public IContainerComputer getContainer( PlayerEntity player )
    {
        if( player == null )
        {
            return null;
        }

        ScreenHandler container = player.currentScreenHandler;
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
        return this.m_instanceID;
    }

    @Override
    public void turnOn()
    {
        // Turn on
        this.m_computer.turnOn();
    }

    // IComputer

    @Override
    public void shutdown()
    {
        // Shutdown
        this.m_computer.shutdown();
    }

    @Override
    public void reboot()
    {
        // Reboot
        this.m_computer.reboot();
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Queue event
        this.m_computer.queueEvent( event, arguments );
    }

    @Override
    public boolean isOn()
    {
        return this.m_computer.isOn();
    }

    @Override
    public boolean isCursorDisplayed()
    {
        return this.m_computer.isOn() && this.m_computer.isBlinking();
    }

    public void sendComputerState( PlayerEntity player )
    {
        // Send state to client
        NetworkHandler.sendToPlayer( player, this.createComputerPacket() );
    }

    public void sendTerminalState( PlayerEntity player )
    {
        // Send terminal state to client
        NetworkHandler.sendToPlayer( player, this.createTerminalPacket() );
    }

    public void broadcastDelete()
    {
        // Send deletion to client
        MinecraftServer server = GameInstanceUtils.getServer();
        if( server != null )
        {
            NetworkHandler.sendToAllPlayers( server, new ComputerDeletedClientMessage( this.getInstanceID() ) );
        }
    }

    public int getID()
    {
        return this.m_computer.getID();
    }

    public void setID( int id )
    {
        this.m_computer.setID( id );
    }

    public String getLabel()
    {
        return this.m_computer.getLabel();
    }

    public void setLabel( String label )
    {
        this.m_computer.setLabel( label );
    }

    public int getRedstoneOutput( ComputerSide side )
    {
        return this.m_computer.getEnvironment()
            .getExternalRedstoneOutput( side );
    }

    public void setRedstoneInput( ComputerSide side, int level )
    {
        this.m_computer.getEnvironment()
            .setRedstoneInput( side, level );
    }

    public int getBundledRedstoneOutput( ComputerSide side )
    {
        return this.m_computer.getEnvironment()
            .getExternalBundledRedstoneOutput( side );
    }

    public void setBundledRedstoneInput( ComputerSide side, int combination )
    {
        this.m_computer.getEnvironment()
            .setBundledRedstoneInput( side, combination );
    }

    public void addAPI( ILuaAPI api )
    {
        this.m_computer.addApi( api );
    }

    // IComputerEnvironment implementation

    public void setPeripheral( ComputerSide side, IPeripheral peripheral )
    {
        this.m_computer.getEnvironment()
            .setPeripheral( side, peripheral );
    }

    public IPeripheral getPeripheral( ComputerSide side )
    {
        return this.m_computer.getEnvironment()
            .getPeripheral( side );
    }

    @Override
    public int getDay()
    {
        return (int) ((this.m_world.getTimeOfDay() + 6000) / 24000) + 1;
    }

    @Override
    public double getTimeOfDay()
    {
        return (this.m_world.getTimeOfDay() + 6000) % 24000 / 1000.0;
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
        return ComputerCraftAPI.createUniqueNumberedSaveDir( this.m_world, "computer" );
    }

    @Override
    public IWritableMount createSaveDirMount( String subPath, long capacity )
    {
        return ComputerCraftAPI.createSaveDirMount( this.m_world, subPath, capacity );
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
