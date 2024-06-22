// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.inventory;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import dan200.computercraft.shared.computer.menu.ServerInputHandler;
import dan200.computercraft.shared.computer.menu.ServerInputState;
import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.container.SingleContainerData;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class AbstractComputerMenu extends AbstractContainerMenu implements ComputerMenu {
    public static final int SIDEBAR_WIDTH = 17;
    private final int uploadMaxSize;

    private final Predicate<Player> canUse;
    private final ComputerFamily family;
    private final ContainerData data;

    private final @Nullable ServerComputer computer;
    private final @Nullable ServerInputState<AbstractComputerMenu> input;

    private final @Nullable NetworkedTerminal terminal;

    private final ItemStack displayStack;

    public AbstractComputerMenu(
        MenuType<? extends AbstractComputerMenu> type, int id, Predicate<Player> canUse,
        ComputerFamily family, @Nullable ServerComputer computer, @Nullable ComputerContainerData containerData
    ) {
        super(type, id);
        this.canUse = canUse;
        this.family = family;
        data = computer == null ? new SimpleContainerData(1) : (SingleContainerData) () -> computer.isOn() ? 1 : 0;
        addDataSlots(data);

        this.computer = computer;
        input = computer == null ? null : new ServerInputState<>(this);
        terminal = containerData == null ? null : containerData.terminal().create();
        displayStack = containerData == null ? ItemStack.EMPTY : containerData.displayStack();
        uploadMaxSize = containerData == null ? Config.uploadMaxSize : containerData.uploadMaxSize();
    }

    @Override
    public boolean stillValid(Player player) {
        return (computer == null || computer.checkUsable(player)) && canUse.test(player);
    }

    public ComputerFamily getFamily() {
        return family;
    }

    public boolean isOn() {
        return data.get(0) != 0;
    }

    public int getUploadMaxSize() {
        return uploadMaxSize;
    }

    @Override
    public ServerComputer getComputer() {
        if (computer == null) throw new UnsupportedOperationException("Cannot access server computer on the client");
        return computer;
    }

    @Override
    public ServerInputHandler getInput() {
        if (input == null) throw new UnsupportedOperationException("Cannot access server computer on the client");
        return input;
    }

    @Override
    public void updateTerminal(TerminalState state) {
        if (terminal == null) throw new UnsupportedOperationException("Cannot update terminal on the server");
        state.apply(terminal);
    }

    /**
     * Get the current terminal state.
     *
     * @return The current terminal state.
     * @throws IllegalStateException When accessed on the server.
     */
    public Terminal getTerminal() {
        if (terminal == null) throw new IllegalStateException("Cannot update terminal on the server");
        return terminal;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (input != null) input.close();
    }

    /**
     * Get the stack associated with this container.
     *
     * @return The current stack.
     */
    public ItemStack getDisplayStack() {
        return displayStack;
    }
}
