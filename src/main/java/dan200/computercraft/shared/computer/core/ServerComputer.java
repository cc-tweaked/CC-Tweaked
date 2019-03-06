/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.ComputerDataClientMessage;
import dan200.computercraft.shared.network.client.ComputerDeletedClientMessage;
import dan200.computercraft.shared.network.client.ComputerTerminalClientMessage;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.InputStream;

public class ServerComputer extends ServerTerminal implements IComputer, IComputerEnvironment
{
    private final int m_instanceID;

    private World m_world;
    private BlockPos m_position;

    private final ComputerFamily m_family;
    private final Computer m_computer;
    private CompoundTag m_userData;
    private boolean m_changed;

    private boolean m_changedLastFrame;
    private int m_ticksSincePing;

    public ServerComputer( World world, int computerID, String label, int instanceID, ComputerFamily family, int terminalWidth, int terminalHeight )
    {
        super( family != ComputerFamily.Normal, terminalWidth, terminalHeight );
        m_instanceID = instanceID;

        m_world = world;
        m_position = null;

        m_family = family;
        m_computer = new Computer( this, getTerminal(), computerID );
        m_computer.setLabel( label );
        m_userData = null;
        m_changed = false;

        m_changedLastFrame = false;
        m_ticksSincePing = 0;
    }

    public ComputerFamily getFamily()
    {
        return m_family;
    }

    public World getWorld()
    {
        return m_world;
    }

    public void setWorld( World world )
    {
        m_world = world;
    }

    public BlockPos getPosition()
    {
        return m_position;
    }

    public void setPosition( BlockPos pos )
    {
        m_position = new BlockPos( pos );
    }

    public IAPIEnvironment getAPIEnvironment()
    {
        return m_computer.getAPIEnvironment();
    }

    public Computer getComputer()
    {
        return m_computer;
    }

    @Override
    public void update()
    {
        super.update();
        m_computer.tick();

        m_changedLastFrame = m_computer.pollAndResetChanged() || m_changed;
        m_changed = false;

        m_ticksSincePing++;
    }

    public void keepAlive()
    {
        m_ticksSincePing = 0;
    }

    public boolean hasTimedOut()
    {
        return m_ticksSincePing > 100;
    }

    public boolean hasOutputChanged()
    {
        return m_changedLastFrame;
    }

    public void unload()
    {
        m_computer.unload();
    }

    public CompoundTag getUserData()
    {
        if( m_userData == null )
        {
            m_userData = new CompoundTag();
        }
        return m_userData;
    }

    public void updateUserData()
    {
        m_changed = true;
    }

    private NetworkMessage createComputerPacket()
    {
        return new ComputerDataClientMessage( this );
    }

    protected NetworkMessage createTerminalPacket()
    {
        CompoundTag tagCompound = new CompoundTag();
        writeDescription( tagCompound );
        return new ComputerTerminalClientMessage( getInstanceID(), tagCompound );
    }

    public void broadcastState( boolean force )
    {
        if( hasOutputChanged() || force )
        {
            // Send computer state to all clients
            NetworkHandler.sendToAllPlayers( createComputerPacket() );
        }

        if( hasTerminalChanged() || force )
        {
            // Send terminal state to clients who are currently interacting with the computer.
            MinecraftServer server = FabricLoader.INSTANCE.getEnvironmentHandler().getServerInstance();

            NetworkMessage packet = createTerminalPacket();
            for( PlayerEntity player : server.getPlayerManager().getPlayerList() )
            {
                if( isInteracting( player ) ) NetworkHandler.sendToPlayer( player, packet );
            }
        }
    }

    public void sendComputerState( PlayerEntity player )
    {
        // Send state to client
        NetworkHandler.sendToPlayer( player, createComputerPacket() );
    }

    public void sendTerminalState( PlayerEntity player )
    {
        // Send terminal state to client
        NetworkHandler.sendToPlayer( player, createTerminalPacket() );
    }

    public void broadcastDelete()
    {
        // Send deletion to client
        NetworkHandler.sendToAllPlayers( new ComputerDeletedClientMessage( getInstanceID() ) );
    }

    public IWritableMount getRootMount()
    {
        return m_computer.getRootMount();
    }

    public int assignID()
    {
        return m_computer.assignID();
    }

    public void setID( int id )
    {
        m_computer.setID( id );
    }

    // IComputer

    @Override
    public int getInstanceID()
    {
        return m_instanceID;
    }

    public int getID()
    {
        return m_computer.getID();
    }

    public String getLabel()
    {
        return m_computer.getLabel();
    }

    @Override
    public boolean isOn()
    {
        return m_computer.isOn();
    }

    @Override
    public boolean isCursorDisplayed()
    {
        return m_computer.isOn() && m_computer.isBlinking();
    }

    @Override
    public void turnOn()
    {
        // Turn on
        m_computer.turnOn();
    }

    @Override
    public void shutdown()
    {
        // Shutdown
        m_computer.shutdown();
    }

    @Override
    public void reboot()
    {
        // Reboot
        m_computer.reboot();
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Queue event
        m_computer.queueEvent( event, arguments );
    }

    public int getRedstoneOutput( int side )
    {
        return m_computer.getEnvironment().getExternalRedstoneOutput( side );
    }

    public void setRedstoneInput( int side, int level )
    {
        m_computer.getEnvironment().setRedstoneInput( side, level );
    }

    public int getBundledRedstoneOutput( int side )
    {
        return m_computer.getEnvironment().getExternalBundledRedstoneOutput( side );
    }

    public void setBundledRedstoneInput( int side, int combination )
    {
        m_computer.getEnvironment().setBundledRedstoneInput( side, combination );
    }

    public void addAPI( ILuaAPI api )
    {
        m_computer.addAPI( api );
    }

    @Deprecated
    public void addAPI( dan200.computercraft.core.apis.ILuaAPI api )
    {
        m_computer.addAPI( api );
    }

    public void setPeripheral( int side, IPeripheral peripheral )
    {
        m_computer.getEnvironment().setPeripheral( side, peripheral );
    }

    public IPeripheral getPeripheral( int side )
    {
        return m_computer.getEnvironment().getPeripheral( side );
    }

    public void setLabel( String label )
    {
        m_computer.setLabel( label );
    }

    // IComputerEnvironment implementation

    @Override
    public double getTimeOfDay()
    {
        return (m_world.getTime() + 6000) % 24000 / 1000.0;
    }

    @Override
    public int getDay()
    {
        return (int) ((m_world.getTime() + 6000) / 24000) + 1;
    }

    @Override
    public IWritableMount createSaveDirMount( String subPath, long capacity )
    {
        return ComputerCraftAPI.createSaveDirMount( m_world, subPath, capacity );
    }

    @Override
    public IMount createResourceMount( String domain, String subPath )
    {
        return ComputerCraftAPI.createResourceMount( domain, subPath );
    }

    @Override
    public InputStream createResourceFile( String domain, String subPath )
    {
        return ComputerCraft.getResourceFile( domain, subPath );
    }

    @Override
    public long getComputerSpaceLimit()
    {
        return ComputerCraft.computerSpaceLimit;
    }

    @Override
    public String getHostString()
    {
        return "ComputerCraft ${version} (Minecraft " + SharedConstants.getGameVersion() + ")";
    }

    @Override
    public int assignNewID()
    {
        return ComputerCraftAPI.createUniqueNumberedSaveDir( m_world, "computer" );
    }

    public boolean isInteracting( PlayerEntity player )
    {
        if( player == null ) return false;

        Container container = player.container;
        if( !(container instanceof IContainerComputer) ) return false;

        IComputer computer = ((IContainerComputer) container).getComputer();
        return computer == this;
    }
}
