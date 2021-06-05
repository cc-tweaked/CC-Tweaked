/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nonnull;
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
    private final NonNullSupplier<String> message;

    public DynamicImageButton(
        Screen screen, int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffTex,
        ResourceLocation texture, int textureWidth, int textureHeight,
        IPressable onPress, String message
    )
    {
        this(
            screen, x, y, width, height, () -> xTexStart, yTexStart, yDiffTex,
            texture, textureWidth, textureHeight,
            onPress, () -> message
        );
    }


    public DynamicImageButton(
        Screen screen, int x, int y, int width, int height, IntSupplier xTexStart, int yTexStart, int yDiffTex,
        ResourceLocation texture, int textureWidth, int textureHeight,
        IPressable onPress, NonNullSupplier<String> message
    )
    {
        super( x, y, width, height, "", onPress );
        this.screen = screen;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.xTexStart = xTexStart;
        this.yTexStart = yTexStart;
        this.yDiffTex = yDiffTex;
        this.texture = texture;
        this.message = message;
    }

    public void renderButton( int mouseX, int mouseY, float partialTicks )
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bind( texture );
        RenderSystem.disableDepthTest();

        int yTex = yTexStart;
        if( isHovered() ) yTex += yDiffTex;

        blit( x, y, xTexStart.getAsInt(), yTex, width, height, textureWidth, textureHeight );
        RenderSystem.enableDepthTest();
    }

    @Nonnull
    @Override
    public String getMessage()
    {
        return I18n.get( message.get() );
    }

    @Override
    public void renderToolTip( int mouseX, int mouseY )
    {
        screen.renderTooltip( getMessage(), mouseX, mouseY );
    }
}
