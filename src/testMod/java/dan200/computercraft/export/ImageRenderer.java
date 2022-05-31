/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.export;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities for saving OpenGL output to an image rather than displaying it on the screen.
 */
public class ImageRenderer implements AutoCloseable
{
    public static final int WIDTH = 64;
    public static final int HEIGHT = 64;

    private final Framebuffer framebuffer = new Framebuffer( WIDTH, HEIGHT, true, Minecraft.ON_OSX );
    private final NativeImage image = new NativeImage( WIDTH, HEIGHT, Minecraft.ON_OSX );

    public ImageRenderer()
    {
        framebuffer.setClearColor( 0, 0, 0, 0 );
        framebuffer.clear( Minecraft.ON_OSX );
    }

    public void setupState()
    {
        RenderSystem.matrixMode( GL11.GL_PROJECTION );
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.ortho( 0, 16, 16, 0, 1000, 3000 );

        RenderSystem.matrixMode( GL11.GL_MODELVIEW );
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.translatef( 0, 0, -2000f );

        FogRenderer.setupNoFog();
    }

    public void clearState()
    {
        RenderSystem.matrixMode( GL11.GL_PROJECTION );
        RenderSystem.popMatrix();

        RenderSystem.matrixMode( GL11.GL_MODELVIEW );
        RenderSystem.popMatrix();
    }

    public void captureRender( Path output, Runnable render ) throws IOException
    {
        Files.createDirectories( output.getParent() );

        framebuffer.bindWrite( true );
        RenderSystem.clear( GL12.GL_COLOR_BUFFER_BIT | GL12.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX );
        render.run();
        framebuffer.unbindWrite();

        framebuffer.bindRead();
        image.downloadTexture( 0, false );
        image.flipY();
        framebuffer.unbindRead();

        image.writeToFile( output );
    }

    @Override
    public void close()
    {
        image.close();
        framebuffer.destroyBuffers();
    }
}
