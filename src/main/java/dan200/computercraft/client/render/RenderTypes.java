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
    private static ShaderInstance terminalShader;

    public static final RenderType TERMINAL_WITHOUT_DEPTH = Types.TERMINAL_WITHOUT_DEPTH;
    public static final RenderType TERMINAL_BLOCKER = Types.TERMINAL_BLOCKER;
    public static final RenderType TERMINAL_WITH_DEPTH = Types.TERMINAL_WITH_DEPTH;
    public static final RenderType MONITOR_TBO = Types.MONITOR_TBO;
    public static final RenderType PRINTOUT_TEXT = Types.PRINTOUT_TEXT;

    /**
     * This looks wrong (it should be POSITION_COLOR_TEX_LIGHTMAP surely!) but the fragment/vertex shader for that
     * appear to entirely ignore the lightmap.
     *
     * Note that vanilla maps do the same, so this isn't unreasonable.
     */
    public static final RenderType PRINTOUT_BACKGROUND = RenderType.text( new ResourceLocation( "computercraft", "textures/gui/printout.png" ) );

    public static final RenderType POSITION_COLOR = Types.POSITION_COLOR;

    @Nonnull
    static MonitorTextureBufferShader getMonitorTextureBufferShader()
    {
        if( monitorTboShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return monitorTboShader;
    }

    @Nonnull
    static ShaderInstance getTerminalShader()
    {
        if( terminalShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return terminalShader;
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

        event.registerShader(
            new ShaderInstance(
                event.getResourceManager(),
                new ResourceLocation( ComputerCraft.MOD_ID, "terminal" ),
                TERMINAL_WITHOUT_DEPTH.format()
            ),
            x -> terminalShader = x
        );
    }

    private static final class Types extends RenderStateShard
    {
        private static final RenderStateShard.TextureStateShard TERM_FONT_TEXTURE = new TextureStateShard(
            FixedWidthFontRenderer.FONT,
            false, false // blur, minimap
        );
        private static final VertexFormat TERM_FORMAT = DefaultVertexFormat.POSITION_COLOR_TEX;
        private static final VertexFormat.Mode TERM_MODE = VertexFormat.Mode.TRIANGLES;
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
            "terminal_without_depth", TERM_FORMAT, TERM_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_BLOCKER = RenderType.create(
            "terminal_blocker", TERM_FORMAT, TERM_MODE, 256,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .setWriteMaskState( DEPTH_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_WITH_DEPTH = RenderType.create(
            "terminal_with_depth", TERM_FORMAT, TERM_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .createCompositeState( false )
        );

        /**
         * A variant of {@link #TERMINAL_WITH_DEPTH} which uses the lightmap rather than rendering fullbright.
         */
        static final RenderType PRINTOUT_TEXT = RenderType.create(
            "printout_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, TERM_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( RenderStateShard.RENDERTYPE_TEXT_SHADER )
                .setLightmapState( RenderStateShard.LIGHTMAP )
                .createCompositeState( false )
        );

        static final RenderType POSITION_COLOR = RenderType.create(
            "position_color", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 128,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setShaderState( POSITION_COLOR_SHADER )
                .createCompositeState( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
