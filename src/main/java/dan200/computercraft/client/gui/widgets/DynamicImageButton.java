/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Version of {@link net.minecraft.client.gui.widget.TexturedButtonWidget} which allows changing some properties
 * dynamically.
 */
public class DynamicImageButton extends ButtonWidget
{
    private final Screen screen;
    private final Identifier texture;
    private final IntSupplier xTexStart;
    private final int yTexStart;
    private final int yDiffTex;
    private final int textureWidth;
    private final int textureHeight;
    private final Supplier<List<Text>> tooltip;

    public DynamicImageButton(
        Screen screen, int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
        Identifier texture, int textureWidth, int textureHeight,
        PressAction onPress, List<Text> tooltip
    )
    {
        this(
            screen, x, y, width, height, () -> xTexStart, yTexStart, yDiffTex,
            texture, textureWidth, textureHeight,
            onPress, () -> tooltip
        );
    }


    public DynamicImageButton(
        Screen screen, int x, int y, int width, int height, IntSupplier xTexStart, int yTexStart, int yDiffTex,
        Identifier texture, int textureWidth, int textureHeight,
        PressAction onPress, Supplier<List<Text>> tooltip
    )
    {
        super( x, y, width, height, LiteralText.EMPTY, onPress );
        this.screen = screen;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.texture = texture;
        this.tooltip = tooltip;
    }

    @Override
    public void renderButton(@Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        RenderSystem.setShaderTexture( 0, texture );
        RenderSystem.disableDepthTest();

        int yTex = yTexStart;
        if( isHovered() ) yTex += yDiffTex;

        drawTexture( stack, x, y, xTexStart.getAsInt(), yTex, width, height, textureWidth, textureHeight );
        RenderSystem.enableDepthTest();

        if( isHovered() ) renderToolTip( stack, mouseX, mouseY );
    }

    @Nonnull
    @Override
    public Text getMessage()
    {
        List<Text> tooltip = this.tooltip.get();
        return tooltip.isEmpty() ? LiteralText.EMPTY : tooltip.get( 0 );
    }

//    @Override
    public void renderToolTip( @Nonnull MatrixStack stack, int mouseX, int mouseY )
    {
        List<Text> tooltip = this.tooltip.get();

        if( !tooltip.isEmpty() )
        {
            screen.renderTooltip( stack, tooltip, mouseX, mouseY );
        }
    }
}
