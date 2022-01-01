/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Version of {@link net.minecraft.client.gui.components.ImageButton} which allows changing some properties
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
    private final NonNullSupplier<List<Component>> tooltip;

    public DynamicImageButton(
        Screen screen, int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
        ResourceLocation texture, int textureWidth, int textureHeight,
        OnPress onPress, List<Component> tooltip
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
        OnPress onPress, NonNullSupplier<List<Component>> tooltip
    )
    {
        super( x, y, width, height, TextComponent.EMPTY, onPress );
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
    public void renderButton( @Nonnull PoseStack stack, int mouseX, int mouseY, float partialTicks )
    {
        RenderSystem.setShaderTexture( 0, texture );
        RenderSystem.disableDepthTest();

        int yTex = yTexStart;
        if( isHoveredOrFocused() ) yTex += yDiffTex;

        blit( stack, x, y, xTexStart.getAsInt(), yTex, width, height, textureWidth, textureHeight );
        RenderSystem.enableDepthTest();

        if( isHovered ) renderToolTip( stack, mouseX, mouseY );
    }

    @Nonnull
    @Override
    public Component getMessage()
    {
        List<Component> tooltip = this.tooltip.get();
        return tooltip.isEmpty() ? TextComponent.EMPTY : tooltip.get( 0 );
    }

    @Override
    public void renderToolTip( @Nonnull PoseStack stack, int mouseX, int mouseY )
    {
        List<Component> tooltip = this.tooltip.get();
        if( !tooltip.isEmpty() )
        {
            screen.renderComponentTooltip( stack, tooltip, mouseX, mouseY );
        }
    }
}
