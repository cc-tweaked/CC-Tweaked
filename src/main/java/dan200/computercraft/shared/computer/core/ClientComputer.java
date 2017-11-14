/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ClientTerminal;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import dan200.computercraft.shared.network.INetworkedThing;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class ClientComputer extends ClientTerminal
    implements IComputer, INetworkedThing
{
    private final int m_instanceID;

    private int m_computerID;
    private String m_label;
    private boolean m_on;
    private boolean m_blinking;
    private boolean m_changed;
    private NBTTagCompound m_userData;

    private boolean m_changedLastFrame;

    public ClientComputer( int instanceID )
    {
        super( false );
        m_instanceID = instanceID;

        m_computerID = -1;
        m_label = null;
        m_on = false;
        m_blinking = false;
        m_changed = true;
        m_userData = null;
        m_changedLastFrame = false;
    }

    @Override
    public void update()
    {
        super.update();
        m_changedLastFrame = m_changed;
        m_changed = false;
    }

    public boolean hasOutputChanged()
    {
        return m_changedLastFrame;
    }

    public NBTTagCompound getUserData()
    {
        return m_userData;
    }

    public void requestState()
    {
        // Request state from server
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.RequestComputerUpdate;
        packet.m_dataInt = new int[] { getInstanceID() };
        ComputerCraft.sendToServer( packet );
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
        return m_computerID;
    }

    @Override
    public String getLabel()
    {
        return m_label;
    }

    @Override
    public boolean isOn()
    {
        return m_on;
    }

    @Override
    public boolean isCursorDisplayed()
    {
        return m_on && m_blinking;
    }

    @Override
    public void turnOn()
    {
        // Send turnOn to server
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.TurnOn;
        packet.m_dataInt = new int[] { m_instanceID };
        ComputerCraft.sendToServer( packet );
    }

    @Override
    public void shutdown()
    {
        // Send shutdown to server
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.Shutdown;
        packet.m_dataInt = new int[] { m_instanceID };
        ComputerCraft.sendToServer( packet );
    }

    @Override
    public void reboot()
    {
        // Send reboot to server
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.Reboot;
        packet.m_dataInt = new int[] { m_instanceID };
        ComputerCraft.sendToServer( packet );
    }

    @Override
    public void queueEvent( String event )
    {
        queueEvent( event, null );
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Send event to server
        ComputerCraftPacket packet = new ComputerCraftPacket();
        packet.m_packetType = ComputerCraftPacket.QueueEvent;
        packet.m_dataInt = new int[] { m_instanceID };
        packet.m_dataString = new String[] { event };
        if( arguments != null )
        {
            packet.m_dataNBT = NBTUtil.encodeObjects( arguments );
        }
        ComputerCraft.sendToServer( packet );
    }

    private void readComputerDescription( NBTTagCompound nbttagcompound )
    {
        int oldID = m_computerID;
        String oldLabel = m_label;
        boolean oldOn = m_on;
        boolean oldBlinking = m_blinking;
        NBTTagCompound oldUserData = m_userData;

        m_computerID = nbttagcompound.getInteger( "id" );
        m_label = nbttagcompound.hasKey( "label" ) ? nbttagcompound.getString( "label" ) : null;
        m_on = nbttagcompound.getBoolean( "on" );
        m_blinking = nbttagcompound.getBoolean( "blinking" );
        if( nbttagcompound.hasKey( "userData" ) )
        {
            m_userData = nbttagcompound.getCompoundTag( "userData" ).copy();
        }
        else
        {
            m_userData = null;
        }

        if( m_computerID != oldID || m_on != oldOn || m_blinking != oldBlinking || !Objects.equal( m_label, oldLabel ) || !Objects.equal( m_userData, oldUserData ) )
        {
            m_changed = true;
        }
    }

    @Override
    public void handlePacket( ComputerCraftPacket packet, EntityPlayer sender )
    {
        switch( packet.m_packetType )
        {
            case ComputerCraftPacket.ComputerChanged:
                readComputerDescription( packet.m_dataNBT );
                break;
            case ComputerCraftPacket.ComputerTerminalChanged:
                readDescription( packet.m_dataNBT );
                break;
        }
    }
}
