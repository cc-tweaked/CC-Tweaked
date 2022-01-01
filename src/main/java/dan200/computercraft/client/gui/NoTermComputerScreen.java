/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;

public class NoTermComputerScreen<T extends ContainerComputerBase> extends Screen implements MenuAccess<T>
{
    private final T menu;
    private WidgetTerminal terminal;

    public NoTermComputerScreen( T menu, Inventory player, Component title )
    {
        super( title );
        this.menu = menu;
    }

    @Nonnull
    @Override
    public T getMenu()
    {
        return menu;
    }

    @Override
    protected void init()
    {
        passEvents = true; // Pass mouse vents through to the game's mouse handler.
        // First ensure we're still grabbing the mouse, so the user can look around. Then reset bits of state that
        // grabbing unsets.
        minecraft.mouseHandler.grabMouse();
        minecraft.screen = this;
        KeyMapping.releaseAll();

        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        terminal = addWidget( new WidgetTerminal( (ClientComputer) menu.getComputer(), 0, 0, ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight ) );
        terminal.visible = false;
        terminal.active = false;
        setFocused( terminal );
    }

    @Override
    public final void removed()
    {
        super.removed();
        minecraft.keyboardHandler.setSendRepeatsToGui( false );
    }

    @Override
    public final void tick()
    {
        super.tick();
        terminal.update();
    }

    @Override
    public boolean mouseScrolled( double pMouseX, double pMouseY, double pDelta )
    {
        minecraft.player.getInventory().swapPaint( pDelta );
        return super.mouseScrolled( pMouseX, pMouseY, pDelta );
    }

    @Override
    public void onClose()
    {
        minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public final boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }

    @Override
    public void render( @Nonnull PoseStack transform, int mouseX, int mouseY, float partialTicks )
    {
        super.render( transform, mouseX, mouseY, partialTicks );

        Font font = minecraft.font;
        List<FormattedCharSequence> lines = font.split( new TranslatableComponent( "gui.computercraft.pocket_computer_overlay" ), (int) (width * 0.8) );
        float y = 10.0f;
        for( FormattedCharSequence line : lines )
        {
            font.drawShadow( transform, line, (float) ((width / 2) - (minecraft.font.width( line ) / 2)), y, 0xFFFFFF );
            y += 9.0f;
        }
    }
}
