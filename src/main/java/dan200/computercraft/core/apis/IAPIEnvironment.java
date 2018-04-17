/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.TrackingField;

public interface IAPIEnvironment
{
    interface IPeripheralChangeListener
    {
        void onPeripheralChanged( int side, IPeripheral newPeripheral );
    }
    
    Computer getComputer();
    int getComputerID();
    IComputerEnvironment getComputerEnvironment();
    Terminal getTerminal();
    FileSystem getFileSystem();
    
    void shutdown();
    void reboot();
    void queueEvent( String event, Object[] args );

    void setOutput( int side, int output );
    int getOutput( int side );
    int getInput( int side );

    void setBundledOutput( int side, int output );
    int getBundledOutput( int side );
    int getBundledInput( int side );
    
    void setPeripheralChangeListener( IPeripheralChangeListener listener );
    IPeripheral getPeripheral( int side );

    String getLabel();
    void setLabel( String label );

    void addTrackingChange( TrackingField field, long change );

    default void addTrackingChange( TrackingField field )
    {
        addTrackingChange( field, 1 );
    }
}
