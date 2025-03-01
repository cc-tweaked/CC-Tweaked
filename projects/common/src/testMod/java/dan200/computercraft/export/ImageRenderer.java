// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.export;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities for saving OpenGL output to an image rather than displaying it on the screen.
 */
public class ImageRenderer implements AutoCloseable {
    public static final int WIDTH = 64;
    public static final int HEIGHT = 64;

    private final TextureTarget framebuffer = new TextureTarget(WIDTH, HEIGHT, true, Minecraft.ON_OSX);
    private final NativeImage image = new NativeImage(WIDTH, HEIGHT, Minecraft.ON_OSX);

    public ImageRenderer() {
        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(Minecraft.ON_OSX);
    }

    public void captureRender(Path output, Runnable render) throws IOException {
        Files.createDirectories(output.getParent());

        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(Minecraft.ON_OSX);
        framebuffer.bindWrite(true);

        // Setup rendering state
        var projectionMatrix = RenderSystem.getProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().identity().ortho(0, 16, 16, 0, 1000, 3000), VertexSorting.ORTHOGRAPHIC_Z);

        var transform = RenderSystem.getModelViewStack();
        transform.pushMatrix();
        transform.identity();
        transform.translate(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();

        Lighting.setupFor3DItems();
        FogRenderer.setupNoFog();

        // Render
        render.run();

        // Restore rendering state
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        framebuffer.unbindWrite();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // And save the image
        framebuffer.bindRead();
        image.downloadTexture(0, false);
        image.flipY();
        framebuffer.unbindRead();

        image.writeToFile(output);
    }

    @Override
    public void close() {
        image.close();
        framebuffer.destroyBuffers();
    }
}
