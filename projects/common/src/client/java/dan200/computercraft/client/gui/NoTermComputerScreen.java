// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * The GUI for off-hand computers. This accepts keyboard input, but does not render a terminal.
 *
 * @param <T> The concrete type of the associated menu.
 */
public class NoTermComputerScreen<T extends AbstractComputerMenu> extends Screen implements MenuAccess<T> {
    private final T menu;
    private final Terminal terminalData;
    private @Nullable TerminalWidget terminal;

    public NoTermComputerScreen(T menu, Inventory player, Component title) {
        super(title);
        this.menu = menu;
        terminalData = menu.getTerminal();
    }

    @Override
    public T getMenu() {
        return menu;
    }

    @Override
    protected void init() {
        passEvents = true; // Pass mouse vents through to the game's mouse handler.
        // First ensure we're still grabbing the mouse, so the user can look around. Then reset bits of state that
        // grabbing unsets.
        minecraft.mouseHandler.grabMouse();
        minecraft.screen = this;
        KeyMapping.releaseAll();

        super.init();

        terminal = addWidget(new TerminalWidget(terminalData, new ClientInputHandler(menu), 0, 0));
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
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        minecraft.player.getInventory().swapPaint(pDelta);
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public void onClose() {
        minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public final boolean keyPressed(int key, int scancode, int modifiers) {
        // Forward the tab key to the terminal, rather than moving between controls.
        if (key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal) {
            return getFocused().keyPressed(key, scancode, modifiers);
        }

        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void render(PoseStack transform, int mouseX, int mouseY, float partialTicks) {
        super.render(transform, mouseX, mouseY, partialTicks);

        var font = minecraft.font;
        var lines = font.split(Component.translatable("gui.computercraft.pocket_computer_overlay"), (int) (width * 0.8));
        var y = 10.0f;
        for (var line : lines) {
            font.drawShadow(transform, line, (float) ((width / 2) - (minecraft.font.width(line) / 2)), y, 0xFFFFFF);
            y += 9.0f;
        }
    }
}
