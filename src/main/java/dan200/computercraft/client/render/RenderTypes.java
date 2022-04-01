/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.io.IOException;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD )
public class RenderTypes
{
    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

    private static MonitorTextureBufferShader monitorTboShader;

    /**
     * Renders a fullbright terminal without writing to the depth layer. This is used in combination with
     * {@link #TERMINAL_BLOCKER} to ensure we can render a terminal without z-fighting.
     */
    public static final RenderType TERMINAL_WITHOUT_DEPTH = Types.TERMINAL_WITHOUT_DEPTH;

    /**
     * A transparent texture which only writes to the depth layer.
     */
    public static final RenderType TERMINAL_BLOCKER = Types.TERMINAL_BLOCKER;

    /**
     * Renders a fullbright terminal which also writes to the depth layer. This is used when z-fighting isn't an issue -
     * for instance rendering an empty terminal or inside a GUI.
     *
     * This is identical to <em>vanilla's</em> {@link RenderType#text}. Forge overrides one with a definition which sets
     * sortOnUpload to true, which is entirely broken!
     */
    public static final RenderType TERMINAL_WITH_DEPTH = Types.TERMINAL_WITH_DEPTH;

    /**
     * Renders a monitor with the TBO shader.
     *
     * @see MonitorTextureBufferShader
     */
    public static final RenderType MONITOR_TBO = Types.MONITOR_TBO;

    /**
     * A variant of {@link #TERMINAL_WITH_DEPTH} which uses the lightmap rather than rendering fullbright.
     */
    public static final RenderType PRINTOUT_TEXT = RenderType.text( FixedWidthFontRenderer.FONT );

    /**
     * Printout's background texture. {@link RenderType#text(ResourceLocation)} is a <em>little</em> questionable, but
     * it is what maps use, so should behave the same as vanilla in both item frames and in-hand.
     */
    public static final RenderType PRINTOUT_BACKGROUND = RenderType.text( new ResourceLocation( "computercraft", "textures/gui/printout.png" ) );

    @Nonnull
    static MonitorTextureBufferShader getMonitorTextureBufferShader()
    {
        if( monitorTboShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return monitorTboShader;
    }

    @Nonnull
    static ShaderInstance getTerminalShader()
    {
        return GameRenderer.getPositionColorTexShader();
    }

    @SubscribeEvent
    public static void registerShaders( RegisterShadersEvent event ) throws IOException
    {
        event.registerShader(
            new MonitorTextureBufferShader(
                event.getResourceManager(),
                new ResourceLocation( ComputerCraft.MOD_ID, "monitor_tbo" ),
                MONITOR_TBO.format()
            ),
            x -> monitorTboShader = (MonitorTextureBufferShader) x
        );
    }

    private static final class Types extends RenderStateShard
    {
        private static final RenderStateShard.TextureStateShard TERM_FONT_TEXTURE = new TextureStateShard(
            FixedWidthFontRenderer.FONT,
            false, false // blur, minimap
        );
        private static final VertexFormat TERM_FORMAT = DefaultVertexFormat.POSITION_COLOR_TEX;
        private static final ShaderStateShard TERM_SHADER = new ShaderStateShard( RenderTypes::getTerminalShader );

        static final RenderType MONITOR_TBO = RenderType.create(
            "monitor_tbo", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLE_STRIP, 128,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( new ShaderStateShard( RenderTypes::getMonitorTextureBufferShader ) )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_WITHOUT_DEPTH = RenderType.create(
            "terminal_without_depth", TERM_FORMAT, VertexFormat.Mode.QUADS, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .setLightmapState( LIGHTMAP )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_BLOCKER = RenderType.create(
            "terminal_blocker", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setShaderState( POSITION_SHADER )
                .setWriteMaskState( DEPTH_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_WITH_DEPTH = RenderType.create(
            "terminal_with_depth", TERM_FORMAT, VertexFormat.Mode.QUADS, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .setLightmapState( LIGHTMAP )
                .createCompositeState( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
