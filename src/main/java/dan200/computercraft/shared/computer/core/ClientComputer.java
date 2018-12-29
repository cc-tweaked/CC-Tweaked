/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ClientTerminal;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import dan200.computercraft.shared.network.server.RequestComputerMessage;
import net.minecraft.nbt.NBTTagCompound;

public class ClientComputer extends ClientTerminal implements IComputer
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
        ComputerCraft.sendToServer( new RequestComputerMessage( getInstanceID() ) );
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
        ComputerCraft.sendToServer( new ComputerActionServerMessage( m_instanceID, ComputerActionServerMessage.Action.TURN_ON ) );
    }

    @Override
    public void shutdown()
    {
        // Send shutdown to server
        ComputerCraft.sendToServer( new ComputerActionServerMessage( m_instanceID, ComputerActionServerMessage.Action.SHUTDOWN ) );
    }

    @Override
    public void reboot()
    {
        // Send reboot to server
        ComputerCraft.sendToServer( new ComputerActionServerMessage( m_instanceID, ComputerActionServerMessage.Action.REBOOT ) );
    }

    @Override
    public void queueEvent( String event, Object[] arguments )
    {
        // Send event to server
        ComputerCraft.sendToServer( new QueueEventServerMessage( m_instanceID, event, arguments ) );
    }

    public void setState( int id, String label, ComputerState state, NBTTagCompound userData )
    {
        int oldID = m_computerID;
        String oldLabel = m_label;
        boolean oldOn = m_on;
        boolean oldBlinking = m_blinking;
        NBTTagCompound oldUserData = m_userData;

        m_computerID = id;
        m_label = label;
        m_on = state != ComputerState.OFF;
        m_blinking = state == ComputerState.BLINKING;
        m_userData = userData;

        m_changed |= m_computerID != oldID || m_on != oldOn || m_blinking != oldBlinking
            || !Objects.equal( m_label, oldLabel ) || !Objects.equal( m_userData, oldUserData );
    }
}
