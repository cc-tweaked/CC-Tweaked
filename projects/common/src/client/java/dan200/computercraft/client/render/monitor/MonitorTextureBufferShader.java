// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render.monitor;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.core.util.Colour;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.getColour;

/**
 * The shader used for the monitor TBO renderer.
 * <p>
 * This extends Minecraft's default shader loading code to extract out the TBO buffer and handle our custom uniforms
 * ({@code MonitorData}, {@code CursorBlink}).
 * <p>
 * See also {@code monitor_tbo.fsh} and {@code monitor_tbo.vsh} in the mod's resources.
 *
 * @see RenderTypes#getMonitorTextureBufferShader()
 */
public class MonitorTextureBufferShader extends ShaderInstance {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorTextureBufferShader.class);

    public static final int UNIFORM_SIZE = 4 * 4 * 16 + 4 + 4 + 2 * 4 + 4;

    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private final int monitorData;
    private int uniformBuffer = 0;

    private final @Nullable Uniform cursorBlink;

    public MonitorTextureBufferShader(ResourceProvider provider, String location, VertexFormat format) throws IOException {
        super(provider, location, format);
        monitorData = GL31.glGetUniformBlockIndex(getId(), "MonitorData");
        if (monitorData == -1) throw new IllegalStateException("Could not find MonitorData uniform.");

        cursorBlink = getUniformChecked("CursorBlink");

        var tbo = getUniformChecked("Tbo");
        if (tbo != null) tbo.set(TEXTURE_INDEX - GL13.GL_TEXTURE0);
    }

    public void setupUniform(int buffer) {
        uniformBuffer = buffer;

        var cursorAlpha = FrameInfo.getGlobalCursorBlink() ? 1 : 0;
        if (cursorBlink != null && cursorBlink.getIntBuffer().get(0) != cursorAlpha) cursorBlink.set(cursorAlpha);
    }

    @Override
    public void apply() {
        super.apply();
        GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, monitorData, uniformBuffer);
    }

    @Nullable
    private Uniform getUniformChecked(String name) {
        var uniform = getUniform(name);
        if (uniform == null) {
            LOG.warn("Monitor shader {} should have uniform {}, but it was not present.", getName(), name);
        }

        return uniform;
    }

    public static void setTerminalData(ByteBuffer buffer, Terminal terminal) {
        int width = terminal.getWidth(), height = terminal.getHeight();

        var pos = 0;
        for (var y = 0; y < height; y++) {
            TextBuffer text = terminal.getLine(y), textColour = terminal.getTextColourLine(y), background = terminal.getBackgroundColourLine(y);
            for (var x = 0; x < width; x++) {
                buffer.put(pos, (byte) (text.charAt(x) & 0xFF));
                buffer.put(pos + 1, (byte) getColour(textColour.charAt(x), Colour.WHITE));
                buffer.put(pos + 2, (byte) getColour(background.charAt(x), Colour.BLACK));
                pos += 3;
            }
        }

        buffer.limit(pos);
    }

    public static void setUniformData(ByteBuffer buffer, Terminal terminal) {
        var pos = 0;
        var palette = terminal.getPalette();
        for (var i = 0; i < 16; i++) {
            {
                var colour = palette.getColour(i);
                if (!terminal.isColour()) {
                    var f = FixedWidthFontRenderer.toGreyscale(colour);
                    buffer.putFloat(pos, f).putFloat(pos + 4, f).putFloat(pos + 8, f);
                } else {
                    buffer.putFloat(pos, (float) colour[0]).putFloat(pos + 4, (float) colour[1]).putFloat(pos + 8, (float) colour[2]);
                }
            }

            pos += 4 * 4; // std140 requires these are 4-wide
        }

        var showCursor = FixedWidthFontRenderer.isCursorVisible(terminal);
        buffer
            .putInt(pos, terminal.getWidth()).putInt(pos + 4, terminal.getHeight())
            .putInt(pos + 8, showCursor ? terminal.getCursorX() : -2)
            .putInt(pos + 12, showCursor ? terminal.getCursorY() : -2)
            .putInt(pos + 16, 15 - terminal.getTextColour());

        buffer.limit(UNIFORM_SIZE);
    }
}
