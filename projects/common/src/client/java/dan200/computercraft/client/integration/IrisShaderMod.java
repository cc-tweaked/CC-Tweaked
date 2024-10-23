// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;
import net.minecraft.client.renderer.LightTexture;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Optional;

@AutoService(ShaderMod.Provider.class)
public class IrisShaderMod implements ShaderMod.Provider {
    @Override
    public Optional<ShaderMod> get() {
        return PlatformHelper.get().isModLoaded("iris") ? Optional.of(new Impl()) : Optional.empty();
    }

    private static final class Impl extends ShaderMod {
        @Override
        public boolean isRenderingShadowPass() {
            return IrisApi.getInstance().isRenderingShadowPass();
        }

        @Override
        public DirectFixedWidthFontRenderer.QuadEmitter getQuadEmitter(int vertexCount, ByteBufferBuilder makeBuffer) {
            return IrisApi.getInstance().getMinorApiRevision() >= 1
                ? new IrisQuadEmitter(vertexCount, makeBuffer)
                : super.getQuadEmitter(vertexCount, makeBuffer);
        }

        private static final class IrisQuadEmitter implements DirectFixedWidthFontRenderer.QuadEmitter {
            private final IrisTextVertexSink sink;
            private @Nullable ByteBuffer buffer;

            private IrisQuadEmitter(int vertexCount, ByteBufferBuilder builder) {
                sink = IrisApi.getInstance().createTextVertexSink(vertexCount, i -> {
                    if (buffer != null) throw new IllegalStateException("Allocated multiple buffers");
                    return buffer = MemoryUtil.memByteBuffer(builder.reserve(i), i);
                });
            }

            @Override
            public void quad(float x1, float y1, float x2, float y2, float z, int colour, float u1, float v1, float u2, float v2) {
                sink.quad(x1, y1, x2, y2, z, colour, u1, v1, u2, v2, LightTexture.FULL_BRIGHT);
            }

            @Override
            public VertexFormat format() {
                return sink.getUnderlyingVertexFormat();
            }
        }
    }
}
