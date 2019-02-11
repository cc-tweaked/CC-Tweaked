/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.computer.IComputerOwned;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.TrackingField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IAPIEnvironment extends IComputerOwned
{
    String[] SIDE_NAMES = new String[] {
        "bottom", "top", "back", "front", "right", "left",
    };

    int SIDE_COUNT = 6;

    interface IPeripheralChangeListener
    {
        void onPeripheralChanged( int side, @Nullable IPeripheral newPeripheral );
    }

    @Nonnull
    @Override
    Computer getComputer();

    int getComputerID();

    @Nonnull
    IComputerEnvironment getComputerEnvironment();

    @Nonnull
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

    void setPeripheralChangeListener( @Nullable IPeripheralChangeListener listener );

    @Nullable
    IPeripheral getPeripheral( int side );

    String getLabel();

    void setLabel( @Nullable String label );

    void addTrackingChange( @Nonnull TrackingField field, long change );

    default void addTrackingChange( @Nonnull TrackingField field )
    {
        addTrackingChange( field, 1 );
    }
}
