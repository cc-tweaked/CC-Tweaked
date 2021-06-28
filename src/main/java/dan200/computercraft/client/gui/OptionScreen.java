/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IBidiRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

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

    private IBidiRenderer messageRenderer;
    private final ITextComponent message;
    private final List<Widget> buttons;
    private final Runnable exit;

    private final Screen originalScreen;

    private OptionScreen( ITextComponent title, ITextComponent message, List<Widget> buttons, Runnable exit, Screen originalScreen )
    {
        super( title );
        this.message = message;
        this.buttons = buttons;
        this.exit = exit;
        this.originalScreen = originalScreen;
    }

    public static void show( Minecraft minecraft, ITextComponent title, ITextComponent message, List<Widget> buttons, Runnable exit )
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

        messageRenderer = IBidiRenderer.create( font, message, innerWidth - PADDING * 2 );

        int textHeight = messageRenderer.getLineCount() * FONT_HEIGHT + PADDING * 2;
        innerHeight = textHeight + (buttons.isEmpty() ? 0 : buttons.get( 0 ).getHeight()) + PADDING;

        x = (width - innerWidth) / 2;
        y = (height - innerHeight) / 2;

        int x = (width - buttonWidth) / 2;
        for( Widget button : buttons )
        {
            button.x = x;
            button.y = y + textHeight;
            addButton( button );

            x += BUTTON_WIDTH + PADDING;
        }
    }

    @Override
    public void render( @Nonnull MatrixStack transform, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( transform );

        // Render the actual texture.
        minecraft.textureManager.bind( BACKGROUND );
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

    public static Widget newButton( ITextComponent component, Button.IPressable clicked )
    {
        return new Button( 0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, component, clicked );
    }

    public void disable()
    {
        for( Widget widget : buttons ) widget.active = false;
    }

    @Nonnull
    public Screen getOriginalScreen()
    {
        return originalScreen;
    }
}
