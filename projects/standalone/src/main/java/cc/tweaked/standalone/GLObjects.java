// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.standalone;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL45C;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.opengl.GL45C.*;

/**
 * A utility class for creating OpenGL objects.
 * <p>
 * This provides the following:
 * <ul>
 *     <li>Provides automatic lifetime management of objects.</li>
 *     <li>Adds additional safety checks (i.e. values were allocated correctly).</li>
 *     <li>Attaches labels to each object, for easier debugging with RenderDoc.</li>
 * </ul>
 * <p>
 * All objects are created using the new Direct State Access (DSA) interface. Consumers should also use DSA when working
 * with these buffers.
 */
public class GLObjects implements AutoCloseable {
    private final Deque<Runnable> toClose = new ArrayDeque<>();

    public void add(Runnable task) {
        toClose.push(task);
    }

    /**
     * Create a new buffer associated with this instance.
     *
     * @param name The debugging name of this buffer.
     * @return The newly created buffer.
     */
    public int createBuffer(String name) {
        var buffer = glCreateBuffers();
        add(() -> glDeleteBuffers(buffer));

        glObjectLabel(GL_BUFFER, buffer, "Buffer - " + name);
        glNamedBufferData(buffer, 0, GL_STATIC_DRAW);
        return buffer;
    }

    /**
     * Create a new vertex array object.
     *
     * @param name The debugging name of this vertex array.
     * @return The newly created vertex array.
     */
    public int createVertexArray(String name) {
        var vbo = glCreateVertexArrays();
        add(() -> glDeleteVertexArrays(vbo));
        glObjectLabel(GL_VERTEX_ARRAY, vbo, "Vertex Array - " + name);
        return vbo;
    }

    /**
     * Create a new texture associated with this instance.
     *
     * @param type The type of this texture, for instance {@link GL45C#GL_TEXTURE_2D}
     * @param name The debugging name of this texture.
     * @return The newly created texture.
     */
    public int createTexture(int type, String name) {
        var texture = glCreateTextures(type);
        add(() -> glDeleteTextures(texture));

        glObjectLabel(GL_TEXTURE, texture, "Texture - " + name);
        return texture;
    }

    /**
     * Create a texture, loading it from an image.
     *
     * @param path The path to the image. This should be on the classpath.
     * @return The newly created texture.
     * @throws IOException If the image could not be found.
     */
    public int loadTexture(String path) throws IOException {
        BufferedImage image;
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Cannot find " + path);
            image = ImageIO.read(stream);
        }

        var textureData = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        for (var y = 0; y < image.getHeight(); y++) {
            for (var x = 0; x < image.getWidth(); x++) {
                var argb = image.getRGB(x, y);
                textureData.put((byte) ((argb >> 16) & 0xFF)).put((byte) ((argb >> 8) & 0xFF)).put((byte) (argb & 0xFF)).put((byte) ((argb >> 24) & 0xFF));
            }
        }
        textureData.flip();

        var texture = createTexture(GL_TEXTURE_2D, path);

        glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTextureStorage2D(texture, 1, GL_RGBA8, image.getWidth(), image.getHeight());

        glTextureSubImage2D(texture, 0, 0, 0, image.getWidth(), image.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, textureData);
        return texture;
    }

    /**
     * Create and compile a shader.
     *
     * @param type The type of this shader, for instance {@link GL45C#GL_FRAGMENT_SHADER}.
     * @param path The path to the shader file. This should be on the classpath.
     * @return The newly created shader.
     * @throws IOException If the shader could not be found.
     */
    public int compileShader(int type, String path) throws IOException {
        String contents;
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Could not load shader " + path);
            contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        var shader = glCreateShader(type);
        if (shader <= 0) throw new IllegalStateException("Could not create shader");
        add(() -> glDeleteShader(shader));

        glObjectLabel(GL_SHADER, shader, "Shader - " + path);

        glShaderSource(shader, contents);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            var error = glGetShaderInfoLog(shader, 32768);
            throw new IllegalStateException("Could not compile shader " + path + ": " + error);
        }


        return shader;
    }

    /**
     * Create a new program.
     *
     * @param name The debugging name of this program.
     * @return The newly created program.
     */
    public int createProgram(String name) {
        var program = glCreateProgram();
        if (program <= 0) throw new IllegalStateException("Could not create shader program");
        add(() -> glDeleteProgram(program));

        glObjectLabel(GL_PROGRAM, program, "Program - " + name);
        return program;
    }

    @Override
    public void close() {
        Runnable close;
        while ((close = toClose.pollLast()) != null) close.run();
    }
}
