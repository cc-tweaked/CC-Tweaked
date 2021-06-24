/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Registers buttons to interact with a computer.
 */
public final class ComputerSidebar
{
    private static final ResourceLocation TEXTURE = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/buttons.png" );

    private static final int TEX_SIZE = 64;

    private static final int ICON_WIDTH = 12;
    private static final int ICON_HEIGHT = 12;
    private static final int ICON_MARGIN = 2;

    private static final int ICON_TEX_Y_DIFF = 14;

    private static final int CORNERS_BORDER = 3;
    private static final int FULL_BORDER = CORNERS_BORDER + ICON_MARGIN;

    private static final int BUTTONS = 2;
    private static final int HEIGHT = (ICON_HEIGHT + ICON_MARGIN * 2) * BUTTONS + CORNERS_BORDER * 2;
    public static final int WIDTH = 17;

    private ComputerSidebar()
    {
    }

    public static void addButtons( Screen screen, ClientComputer computer, Consumer<Widget> add, int x, int y )
    {
        x += CORNERS_BORDER + 1;
        y += CORNERS_BORDER + ICON_MARGIN;

        add.accept( new DynamicImageButton(
            screen, x, y, ICON_WIDTH, ICON_HEIGHT, () -> computer.isOn() ? 15 : 1, 1, ICON_TEX_Y_DIFF,
            TEXTURE, TEX_SIZE, TEX_SIZE, b -> toggleComputer( computer ),
            () -> computer.isOn() ? Arrays.asList(
                new TranslationTextComponent( "gui.computercraft.tooltip.turn_off" ),
                new TranslationTextComponent( "gui.computercraft.tooltip.turn_off.key" ).withStyle( TextFormatting.GRAY )
            ) : Arrays.asList(
                new TranslationTextComponent( "gui.computercraft.tooltip.turn_on" ),
                new TranslationTextComponent( "gui.computercraft.tooltip.turn_off.key" ).withStyle( TextFormatting.GRAY )
            )
        ) );

        y += ICON_HEIGHT + ICON_MARGIN * 2;

        add.accept( new DynamicImageButton(
            screen, x, y, ICON_WIDTH, ICON_HEIGHT, 29, 1, ICON_TEX_Y_DIFF,
            TEXTURE, TEX_SIZE, TEX_SIZE, b -> computer.queueEvent( "terminate" ),
            Arrays.asList(
                new TranslationTextComponent( "gui.computercraft.tooltip.terminate" ),
                new TranslationTextComponent( "gui.computercraft.tooltip.terminate.key" ).withStyle( TextFormatting.GRAY )
            )
        ) );
    }

    public static void renderBackground( MatrixStack transform, int x, int y )
    {
        Screen.blit( transform,
            x, y, 0, 102, WIDTH, FULL_BORDER,
            ComputerBorderRenderer.TEX_SIZE, ComputerBorderRenderer.TEX_SIZE
        );

        Screen.blit( transform,
            x, y + FULL_BORDER, WIDTH, HEIGHT - FULL_BORDER * 2,
            0, 107, WIDTH, 4,
            ComputerBorderRenderer.TEX_SIZE, ComputerBorderRenderer.TEX_SIZE
        );

        Screen.blit( transform,
            x, y + HEIGHT - FULL_BORDER, 0, 111, WIDTH, FULL_BORDER,
            ComputerBorderRenderer.TEX_SIZE, ComputerBorderRenderer.TEX_SIZE
        );
    }

    private static void toggleComputer( ClientComputer computer )
    {
        if( computer.isOn() )
        {
            computer.shutdown();
        }
        else
        {
            computer.turnOn();
        }
    }
}
