// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.gui;

import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.core.input.UserComputerInput;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * The GUI for off-hand computers. This accepts keyboard input, but does not render a terminal.
 *
 * @param <T> The concrete type of the associated menu.
 */
public class NoTermComputerScreen<T extends AbstractComputerMenu> extends Screen implements MenuAccess<T> {
    private final T menu;
    protected final UserComputerInput computerInput;
    protected final ClientComputerActions computerActions;
    private @Nullable TerminalWidget terminal;

    private final ScrollWheelHandler scrollHandler = new ScrollWheelHandler();

    public NoTermComputerScreen(T menu, Inventory player, Component title) {
        super(title);
        this.menu = menu;
        computerInput = new UserComputerInput(new ClientComputerInput(menu), menu.getTerminal());
        computerActions = new ClientComputerActions(menu);
    }

    @Override
    public T getMenu() {
        return menu;
    }

    @Override
    protected void init() {
        // First ensure we're still grabbing the mouse, so the user can look around. Then reset bits of state that
        // grabbing unsets.
        minecraft.mouseHandler.grabMouse();
        minecraft.screen = this;
        KeyMapping.releaseAll();

        super.init();

        terminal = addWidget(new TerminalWidget(menu.getTerminal(), computerInput, computerActions, 0, 0));
        terminal.visible = false;
        terminal.active = false;
        setFocused(terminal);
    }

    @Override
    public final void tick() {
        super.tick();
        assertNonNull(terminal).update();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var direction = scrollHandler.onMouseScroll(scrollX, scrollY);
        var inventory = Objects.requireNonNull(minecraft.player).getInventory();
        inventory.setSelectedSlot(ScrollWheelHandler.getNextScrollWheelSelection(
            direction.y == 0 ? -direction.x : direction.y, inventory.getSelectedSlot(), Inventory.getSelectionSize()
        ));

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Objects.requireNonNull(minecraft.player).closeContainer();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        // Forward the tab key to the terminal, rather than moving between controls.
        if (event.key() == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal) {
            return getFocused().keyPressed(event);
        }

        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        var font = minecraft.font;
        var lines = font.split(Component.translatable("gui.computercraft.pocket_computer_overlay"), (int) (width * 0.8));
        var y = 10;
        for (var line : lines) {
            graphics.drawString(font, line, (width / 2) - (font.width(line) / 2), y, 0xFFFFFFFF, true);
            y += 9;
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Skip rendering the background.
    }
}
