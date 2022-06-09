/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.export;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
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

    private final TextureTarget framebuffer = new TextureTarget( WIDTH, HEIGHT, true, Minecraft.ON_OSX );
    private final NativeImage image = new NativeImage( WIDTH, HEIGHT, Minecraft.ON_OSX );

    private Matrix4f projectionMatrix;

    public ImageRenderer()
    {
        framebuffer.setClearColor( 0, 0, 0, 0 );
        framebuffer.clear( Minecraft.ON_OSX );
    }

    public void setupState()
    {
        projectionMatrix = RenderSystem.getProjectionMatrix();
        RenderSystem.setProjectionMatrix( Matrix4f.orthographic( 0, 16, 0, 16, 1000, 3000 ) );

        var transform = RenderSystem.getModelViewStack();
        transform.setIdentity();
        transform.translate( 0.0f, 0.0f, -2000.0f );

        FogRenderer.setupNoFog();
    }

    public void clearState()
    {
        RenderSystem.setProjectionMatrix( projectionMatrix );
        RenderSystem.getModelViewStack().popPose();
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
