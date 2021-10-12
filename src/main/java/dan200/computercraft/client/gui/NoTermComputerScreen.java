/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;

public class NoTermComputerScreen<T extends ContainerComputerBase> extends Screen implements ScreenHandlerProvider<T>
{
    private final T menu;
    private WidgetTerminal terminal;

    public NoTermComputerScreen( T menu, PlayerInventory player, Text title )
    {
        super( title );
        this.menu = menu;
    }

    @Nonnull
    @Override
    public T getScreenHandler()
    {
        return menu;
    }

    @Override
    protected void init()
    {
        this.passEvents = true;
        client.mouse.lockCursor();
        client.currentScreen = this;
        super.init();
        client.keyboard.setRepeatEvents( true );

        terminal = addSelectableChild( new WidgetTerminal( (ClientComputer) menu.getComputer(), 0, 0, ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight ) );
        terminal.visible = false;
        terminal.active = false;
        setFocused( terminal );
    }

    @Override
    public final void removed()
    {
        super.removed();
        client.keyboard.setRepeatEvents( false );
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
        client.player.getInventory().scrollInHotbar( pDelta );
        return super.mouseScrolled( pMouseX, pMouseY, pDelta );
    }

    @Override
    public void onClose()
    {
        client.player.closeHandledScreen();
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
    public void render( MatrixStack transform, int mouseX, int mouseY, float partialTicks )
    {
        super.render( transform, mouseX, mouseY, partialTicks );

        TextRenderer font = client.textRenderer;
        List<OrderedText> lines = font.wrapLines( new TranslatableText( "gui.computercraft.pocket_computer_overlay" ), (int) (width * 0.8) );
        float y = 10.0f;
        for( OrderedText line : lines )
        {
            font.drawWithShadow( transform, line, (float) ((width / 2) - (client.textRenderer.getWidth( line ) / 2)), y, 0xFFFFFF );
            y += 9.0f;
        }
    }
}
