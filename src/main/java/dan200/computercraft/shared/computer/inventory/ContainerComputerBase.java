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
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.computer.menu.ServerInputState;
import dan200.computercraft.shared.network.client.TerminalState;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.SingleIntArray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class ContainerComputerBase extends AbstractContainerMenu implements ComputerMenu
{
    private final Predicate<Player> canUse;
    private final ComputerFamily family;
    private final ContainerData data;

    private final @Nullable ServerComputer computer;
    private final @Nullable ServerInputState<ContainerComputerBase> input;

    private final @Nullable Terminal terminal;

    private final ItemStack displayStack;

    public ContainerComputerBase(
        MenuType<? extends ContainerComputerBase> type, int id, Predicate<Player> canUse,
        ComputerFamily family, @Nullable ServerComputer computer, @Nullable ComputerContainerData containerData
    )
    {
        super( type, id );
        this.canUse = canUse;
        this.family = family;
        data = computer == null ? new SimpleContainerData( 1 ) : (SingleIntArray) () -> computer.isOn() ? 1 : 0;
        addDataSlots( data );

        this.computer = computer;
        input = computer == null ? null : new ServerInputState<>( this );
        terminal = containerData == null ? null : containerData.terminal().create();
        displayStack = containerData == null ? null : containerData.displayStack();
    }

    @Override
    public boolean stillValid( @Nonnull Player player )
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
    public ServerInputHandler getInput()
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
    public void removed( @Nonnull Player player )
    {
        super.removed( player );
        if( input != null ) input.close();
    }

    /**
     * Get the stack associated with this container.
     *
     * @return The current stack.
     */
    @Nonnull
    public ItemStack getDisplayStack()
    {
        return displayStack;
    }
}
