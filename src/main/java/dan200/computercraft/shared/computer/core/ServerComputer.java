/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.shared.common.ServerTerminal;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import dan200.computercraft.shared.network.INetworkedThing;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

import java.io.InputStream;

public class ServerComputer extends ServerTerminal
    implements IComputer, IComputerEnvironment, INetworkedThing
{
    private final int m_instanceID;

    private World m_world;
    private BlockPos m_position;

    private final ComputerFamily m_family;
    private final Computer m_computer;
    private NBTTagCompound m_userData;
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

    public ComputerFamily getFamily(){
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

    @Override
    public void update()
    {
        super.update();
        m_computer.advance( 0.05 );

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

    public NBTTagCompound getUserData()
    {
        if( m_userData == null )
        {
            m_userData = new NBTTagCompound();
        }
        return m_userData;
    }

    public void updateUserData()
    {
        m_changed = true;
    }
    
    private ComputerCraftPacket createComputerPacket() {
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.ComputerChanged;
        packet.m_dataInt = new int[] { getInstanceID() };
        packet.m_dataNBT = new NBTTagCompound();
        writeComputerDescription( packet.m_dataNBT );
        return packet;
    }

    private ComputerCraftPacket createTerminalPacket() {
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.ComputerTerminalChanged;
        packet.m_dataInt = new int[] { getInstanceID() };
        packet.m_dataNBT = new NBTTagCompound();
        writeDescription( packet.m_dataNBT );
        return packet;
    }

    public void broadcastState(boolean force)
    {
        if(hasOutputChanged() || force)
        {
            // Send computer state to all clients
            ComputerCraft.sendToAllPlayers( createComputerPacket() );
        }

        if( hasTerminalChanged() || force )
        {
            // Send terminal state to clients who are currently interacting with the computer.
            FMLCommonHandler handler = FMLCommonHandler.instance();
            if( handler != null )
            {
                ComputerCraftPacket packet = createTerminalPacket();
                MinecraftServer server = handler.getMinecraftServerInstance();
                for( EntityPlayerMP player : server.getPlayerList().getPlayers() )
                {
                    if( isInteracting( player ) )
                    {
                        ComputerCraft.sendToPlayer( player, packet );
                    }
                }
            }
        }
    }

    public void sendComputerState( EntityPlayer player )
    {
        // Send state to client
        ComputerCraft.sendToPlayer( player, createComputerPacket() );
    }

    public void sendTerminalState( EntityPlayer player )
    {
        // Send terminal state to client
        ComputerCraft.sendToPlayer( player, createTerminalPacket() );
    }

    public void broadcastDelete()
    {
        // Send deletion to client
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.ComputerDeleted;
        packet.m_dataInt = new int[] { getInstanceID() };
        ComputerCraft.sendToAllPlayers( packet );
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

    @Override
    public int getID()
    {
        return m_computer.getID();
    }

    @Override
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
    public void queueEvent( String event )
    {
        // Queue event
        queueEvent( event, null );
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Queue event
        m_computer.queueEvent( event, arguments );
    }

    public int getRedstoneOutput( int side )
    {
        return m_computer.getRedstoneOutput( side );
    }

    public void setRedstoneInput( int side, int level )
    {
        m_computer.setRedstoneInput( side, level );
    }

    public int getBundledRedstoneOutput( int side )
    {
        return m_computer.getBundledRedstoneOutput( side );
    }

    public void setBundledRedstoneInput( int side, int combination )
    {
        m_computer.setBundledRedstoneInput( side, combination );
    }

    public void addAPI( ILuaAPI api )
    {
        m_computer.addAPI( api );
    }

    public void setPeripheral( int side, IPeripheral peripheral )
    {
        m_computer.setPeripheral( side, peripheral );
    }

    public IPeripheral getPeripheral( int side )
    {
        return m_computer.getPeripheral( side );
    }

    public void setLabel( String label )
    {
        m_computer.setLabel( label );
    }

    // IComputerEnvironment implementation

    @Override
    public double getTimeOfDay()
    {
        return (m_world.getWorldTime() + 6000) % 24000 / 1000.0;
    }

    @Override
    public int getDay()
    {
        return (int)((m_world.getWorldTime() + 6000) / 24000) + 1;
    }

    @Override
    public IWritableMount createSaveDirMount( String subPath, long capacity )
    {
        return ComputerCraftAPI.createSaveDirMount( m_world, subPath, capacity );
    }

    @Override
    public IMount createResourceMount( String domain, String subPath )
    {
        return ComputerCraftAPI.createResourceMount( ComputerCraft.class, domain, subPath );
    }

    @Override
    public InputStream createResourceFile( String domain, String subPath )
    {
        return ComputerCraft.getResourceFile( ComputerCraft.class, domain, subPath );
    }

    @Override
    public long getComputerSpaceLimit()
    {
        return ComputerCraft.computerSpaceLimit;
    }

    @Override
    public String getHostString()
    {
        return "ComputerCraft ${version} (Minecraft " + Loader.MC_VERSION + ")";
    }

    @Override
    public int assignNewID()
    {
        return ComputerCraft.createUniqueNumberedSaveDir( m_world, "computer" );
    }

    // Networking stuff
    public void writeComputerDescription( NBTTagCompound nbttagcompound )
    {
        nbttagcompound.setInteger( "id", m_computer.getID() );
        String label = m_computer.getLabel();
        if( label != null )
        {
            nbttagcompound.setString( "label", label );
        }
        nbttagcompound.setBoolean( "on", m_computer.isOn() );
        nbttagcompound.setBoolean( "blinking", m_computer.isBlinking() );
        if( m_userData != null )
        {
            nbttagcompound.setTag( "userData", m_userData.copy() );
        }
    }

    // INetworkedThing

    @Override
    public void handlePacket( ComputerCraftPacket packet, EntityPlayer sender )
    {
        // Allow Computer/Tile updates as they may happen at any time.
        if( packet.requiresContainer() && !isInteracting( sender ) )
        {
            return;
        }

        // Receive packets sent from the client to the server
        switch( packet.m_packetType )
        {
            case ComputerCraftPacket.TurnOn:
            {
                // A player has turned the computer on
                turnOn();
                break;
            }
            case ComputerCraftPacket.Reboot:
            {
                // A player has held down ctrl+r
                reboot();
                break;
            }
            case ComputerCraftPacket.Shutdown:
            {
                // A player has held down ctrl+s
                shutdown();
                break;
            }
            case ComputerCraftPacket.QueueEvent:
            {
                // A player has caused a UI event to be fired
                String event = packet.m_dataString[0];
                Object[] arguments = null;
                if( packet.m_dataNBT != null )
                {
                    arguments = NBTUtil.decodeObjects( packet.m_dataNBT );
                }
                queueEvent( event, arguments );
                break;
            }
            case ComputerCraftPacket.SetLabel:
            {
                // A player wants to relabel a computer
                String label = (packet.m_dataString != null && packet.m_dataString.length >= 1) ? packet.m_dataString[0] : null;
                setLabel( label );
                break;
            }
            case ComputerCraftPacket.RequestComputerUpdate:
            {
                // A player asked for an update on the state of the terminal
                sendComputerState( sender );
                break;
            }
        }
    }

    private boolean isInteracting( EntityPlayer player )
    {
        if( player == null ) return false;

        Container container = player.openContainer;
        if( !(container instanceof IContainerComputer) ) return false;

        IComputer computer = ((IContainerComputer) container).getComputer();
        return computer == this;
    }
}
