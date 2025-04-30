// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.export;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
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

    private final TextureTarget framebuffer = new TextureTarget("Export", WIDTH, HEIGHT, true);
    private final NativeImage image = new NativeImage(WIDTH, HEIGHT, true);

    public ImageRenderer() {
        // framebuffer.setFilterMode(0, 0, 0, 0);
        // framebuffer.clear();
    }

    public void captureRender(Path output, Runnable render) throws IOException {
        Files.createDirectories(output.getParent());

        // RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        // framebuffer.bindWrite(true);

        // Setup rendering state
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().identity().ortho(0, 16, 16, 0, 1000, 3000), ProjectionType.ORTHOGRAPHIC);

        var transform = RenderSystem.getModelViewStack();
        transform.pushMatrix();
        transform.identity();
        transform.translate(0.0f, 0.0f, -2000.0f);

        // Render
        render.run();

        // Restore rendering state
        RenderSystem.restoreProjectionMatrix();
        transform.popMatrix();

        // framebuffer.unbindWrite();
        // Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // And save the image
        // framebuffer.bindRead();
        // image.downloadTexture(0, false);
        // image.flipY();
        // framebuffer.unbindRead();

        image.writeToFile(output);
    }

    @Override
    public void close() {
        image.close();
        framebuffer.destroyBuffers();
    }
}
