/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RenderTypes
{

    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

    @Nullable
    public static MonitorTextureBufferShader monitorTboShader;

    @Nullable
    public static ShaderInstance terminalShader;

    public static final RenderType TERMINAL_WITHOUT_DEPTH = Types.TERMINAL_WITHOUT_DEPTH;
    public static final RenderType MONITOR_TBO = Types.MONITOR_TBO;
    public static final RenderType TERMINAL_BLOCKER = Types.BLOCKER;
    public static final RenderType TERMINAL_WITH_DEPTH = Types.TERMINAL_WITH_DEPTH;
    public static final RenderType PRINTOUT_TEXT = Types.PRINTOUT_TEXT;

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

    private static final class Types extends RenderStateShard
    {
        private static final VertexFormat.Mode GL_MODE = VertexFormat.Mode.TRIANGLES;
        private static final VertexFormat FORMAT = DefaultVertexFormat.POSITION_COLOR_TEX;
        private static final ShaderStateShard TERM_SHADER = new ShaderStateShard( RenderTypes::getTerminalShader );

        private static final RenderStateShard.TextureStateShard TERM_FONT_TEXTURE = new RenderStateShard.TextureStateShard(
            FixedWidthFontRenderer.FONT,
            false, false // blur, minimap
        );

        public static final RenderType MONITOR_TBO = RenderType.create( "monitor_tbo", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLE_STRIP, 128, false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE ) // blur, minimap
                .setShaderState( new RenderStateShard.ShaderStateShard( RenderTypes::getMonitorTextureBufferShader ) )
                .setWriteMaskState( RenderType.COLOR_DEPTH_WRITE )
                .createCompositeState( false ) );

        static final RenderType TERMINAL_WITHOUT_DEPTH = RenderType.create(
            "terminal_without_depth", FORMAT, GL_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType BLOCKER = RenderType.create( "terminal_blocker", FORMAT, GL_MODE, 256, false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .setWriteMaskState( DEPTH_WRITE )
                .createCompositeState( false ) );

        static final RenderType TERMINAL_WITH_DEPTH = RenderType.create(
            "terminal_with_depth", FORMAT, GL_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( TERM_SHADER )
                .createCompositeState( false )
        );

        static final RenderType PRINTOUT_TEXT = RenderType.create(
            "printout_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, GL_MODE, 1024,
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
