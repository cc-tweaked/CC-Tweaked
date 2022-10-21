/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputState;
import dan200.computercraft.shared.network.client.TerminalState;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.SingleIntArray;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class ContainerComputerBase extends Container implements ComputerMenu
{
    private final Predicate<PlayerEntity> canUse;
    private final ComputerFamily family;
    private final IIntArray data;

    private final @Nullable ServerComputer computer;
    private final @Nullable ServerInputState input;

    private final @Nullable Terminal terminal;

    public ContainerComputerBase(
        ContainerType<? extends ContainerComputerBase> type, int id, Predicate<PlayerEntity> canUse,
        ComputerFamily family, @Nullable ServerComputer computer, @Nullable ComputerContainerData containerData
    )
    {
        super( type, id );
        this.canUse = canUse;
        this.family = family;
        data = computer == null ? new IntArray( 1 ) : (SingleIntArray) () -> computer.isOn() ? 1 : 0;
        addDataSlots( data );

        this.computer = computer;
        input = computer == null ? null : new ServerInputState( this );
        terminal = containerData == null ? null : containerData.terminal().create();
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity player )
    {
        return canUse.test( player );
    }

    @Nonnull
    public ComputerFamily getFamily()
    {
        return family;
    }

    public boolean isOn()
    {
        return data.get( 0 ) != 0;
    }

    @Override
    public ServerComputer getComputer()
    {
        if( computer == null ) throw new UnsupportedOperationException( "Cannot access server computer on the client" );
        return computer;
    }

    @Override
    public ServerInputState getInput()
    {
        if( input == null ) throw new UnsupportedOperationException( "Cannot access server computer on the client" );
        return input;
    }

    @Override
    public void updateTerminal( TerminalState state )
    {
        if( terminal == null ) throw new UnsupportedOperationException( "Cannot update terminal on the server" );
        state.apply( terminal );
    }

    /**
     * Get the current terminal state.
     *
     * @return The current terminal state.
     * @throws IllegalStateException When accessed on the server.
     */
    public Terminal getTerminal()
    {
        if( terminal == null ) throw new IllegalStateException( "Cannot update terminal on the server" );
        return terminal;
    }

    @Override
    public void removed( @Nonnull PlayerEntity player )
    {
        super.removed( player );
        if( input != null ) input.close();
    }
}
