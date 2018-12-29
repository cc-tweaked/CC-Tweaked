/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.core;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.ClientTerminal;
import dan200.computercraft.shared.computer.blocks.ComputerState;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import dan200.computercraft.shared.network.server.RequestComputerMessage;
import net.minecraft.nbt.NBTTagCompound;

public class ClientComputer extends ClientTerminal implements IComputer
{
    private final int m_instanceID;

    private boolean m_on = false;
    private boolean m_blinking = false;
    private boolean m_changed = true;
    private NBTTagCompound m_userData = null;

    private boolean m_changedLastFrame = false;

    public ClientComputer( int instanceID )
    {
        super( false );
        m_instanceID = instanceID;
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

    public void setState( ComputerState state, NBTTagCompound userData )
    {
        boolean oldOn = m_on;
        boolean oldBlinking = m_blinking;
        NBTTagCompound oldUserData = m_userData;

        m_on = state != ComputerState.Off;
        m_blinking = state == ComputerState.Blinking;
        m_userData = userData;

        m_changed |= m_on != oldOn || m_blinking != oldBlinking || !Objects.equal( m_userData, oldUserData );
    }
}
