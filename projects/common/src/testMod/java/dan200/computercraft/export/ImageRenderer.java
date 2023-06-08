// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.export;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
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

    private @Nullable Matrix4f projectionMatrix;

    public ImageRenderer() {
        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(Minecraft.ON_OSX);
    }

    public void setupState() {
        projectionMatrix = RenderSystem.getProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().identity().ortho(0, 16, 0, 16, 1000, 3000), VertexSorting.DISTANCE_TO_ORIGIN);

        var transform = RenderSystem.getModelViewStack();
        transform.pushPose();
        transform.setIdentity();
        transform.translate(0.0f, 0.0f, -2000.0f);

        FogRenderer.setupNoFog();
    }

    public void clearState() {
        if (projectionMatrix == null) throw new IllegalStateException("Not currently rendering");
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.getModelViewStack().popPose();
    }

    public void captureRender(Path output, Runnable render) throws IOException {
        Files.createDirectories(output.getParent());

        framebuffer.bindWrite(true);
        RenderSystem.clear(GL12.GL_COLOR_BUFFER_BIT | GL12.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        render.run();
        framebuffer.unbindWrite();

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
