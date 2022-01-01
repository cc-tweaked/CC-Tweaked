/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Version of {@link net.minecraft.client.gui.widget.button.ImageButton} which allows changing some properties
 * dynamically.
 */
public class DynamicImageButton extends Button
{
    private final Screen screen;
    private final ResourceLocation texture;
    private final IntSupplier xTexStart;
    private final int yTexStart;
    private final int yDiffTex;
    private final int textureWidth;
    private final int textureHeight;
    private final NonNullSupplier<List<ITextComponent>> tooltip;

    public DynamicImageButton(
        Screen screen, int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
        ResourceLocation texture, int textureWidth, int textureHeight,
        IPressable onPress, List<ITextComponent> tooltip
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
        ResourceLocation texture, int textureWidth, int textureHeight,
        IPressable onPress, NonNullSupplier<List<ITextComponent>> tooltip
    )
    {
        super( x, y, width, height, StringTextComponent.EMPTY, onPress );
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
    public void renderButton( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bind( texture );
        RenderSystem.disableDepthTest();

        int yTex = yTexStart;
        if( isHovered() ) yTex += yDiffTex;

        blit( stack, x, y, xTexStart.getAsInt(), yTex, width, height, textureWidth, textureHeight );
        RenderSystem.enableDepthTest();

        if( isHovered() ) renderToolTip( stack, mouseX, mouseY );
    }

    @Nonnull
    @Override
    public ITextComponent getMessage()
    {
        List<ITextComponent> tooltip = this.tooltip.get();
        return tooltip.isEmpty() ? StringTextComponent.EMPTY : tooltip.get( 0 );
    }

    @Override
    public void renderToolTip( @Nonnull MatrixStack stack, int mouseX, int mouseY )
    {
        List<ITextComponent> tooltip = this.tooltip.get();
        if( !tooltip.isEmpty() )
        {
            screen.renderWrappedToolTip( stack, tooltip, mouseX, mouseY, screen.getMinecraft().font );
        }
    }
}
