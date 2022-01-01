/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.List;

public final class OptionScreen extends Screen
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation( "computercraft", "textures/gui/blank_screen.png" );

    public static final int BUTTON_WIDTH = 100;
    public static final int BUTTON_HEIGHT = 20;

    private static final int PADDING = 16;
    private static final int FONT_HEIGHT = 9;

    private int x;
    private int y;
    private int innerWidth;
    private int innerHeight;

    private MultiLineLabel messageRenderer;
    private final Component message;
    private final List<AbstractWidget> buttons;
    private final Runnable exit;

    private final Screen originalScreen;

    private OptionScreen( Component title, Component message, List<AbstractWidget> buttons, Runnable exit, Screen originalScreen )
    {
        super( title );
        this.message = message;
        this.buttons = buttons;
        this.exit = exit;
        this.originalScreen = originalScreen;
    }

    public static void show( Minecraft minecraft, Component title, Component message, List<AbstractWidget> buttons, Runnable exit )
    {
        minecraft.setScreen( new OptionScreen( title, message, buttons, exit, unwrap( minecraft.screen ) ) );
    }

    public static Screen unwrap( Screen screen )
    {
        return screen instanceof OptionScreen ? ((OptionScreen) screen).getOriginalScreen() : screen;
    }

    @Override
    public void init()
    {
        super.init();

        int buttonWidth = BUTTON_WIDTH * buttons.size() + PADDING * (buttons.size() - 1);
        int innerWidth = this.innerWidth = Math.max( 256, buttonWidth + PADDING * 2 );

        messageRenderer = MultiLineLabel.create( font, message, innerWidth - PADDING * 2 );

        int textHeight = messageRenderer.getLineCount() * FONT_HEIGHT + PADDING * 2;
        innerHeight = textHeight + (buttons.isEmpty() ? 0 : buttons.get( 0 ).getHeight()) + PADDING;

        x = (width - innerWidth) / 2;
        y = (height - innerHeight) / 2;

        int x = (width - buttonWidth) / 2;
        for( AbstractWidget button : buttons )
        {
            button.x = x;
            button.y = y + textHeight;
            addRenderableWidget( button );

            x += BUTTON_WIDTH + PADDING;
        }
    }

    @Override
    public void render( @Nonnull PoseStack transform, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( transform );

        // Render the actual texture.
        RenderSystem.setShaderTexture( 0, BACKGROUND );
        blit( transform, x, y, 0, 0, innerWidth, PADDING );
        blit( transform,
            x, y + PADDING, 0, PADDING, innerWidth, innerHeight - PADDING * 2,
            innerWidth, PADDING
        );
        blit( transform, x, y + innerHeight - PADDING, 0, 256 - PADDING, innerWidth, PADDING );

        messageRenderer.renderLeftAlignedNoShadow( transform, x + PADDING, y + PADDING, FONT_HEIGHT, 0x404040 );
        super.render( transform, mouseX, mouseY, partialTicks );
    }

    @Override
    public void onClose()
    {
        exit.run();
    }

    public static AbstractWidget newButton( Component component, Button.OnPress clicked )
    {
        return new Button( 0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, component, clicked );
    }

    public void disable()
    {
        for( AbstractWidget widget : buttons ) widget.active = false;
    }

    @Nonnull
    public Screen getOriginalScreen()
    {
        return originalScreen;
    }
}
