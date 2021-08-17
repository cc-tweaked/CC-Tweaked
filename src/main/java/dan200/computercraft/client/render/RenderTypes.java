/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import net.minecraft.Util;
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
import java.util.function.Function;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD )
public class RenderTypes
{
    private static MonitorTextureBufferShader monitorTboShader;
    private static ShaderInstance terminalShader;

    public static final RenderType TERMINAL_WITHOUT_DEPTH = Types.TERMINAL_WITHOUT_DEPTH;
    public static final RenderType TERMINAL_BLOCKER = Types.TERMINAL_BLOCKER;
    public static final RenderType TERMINAL_WITH_DEPTH = Types.TERMINAL_WITH_DEPTH;
    public static final RenderType MONITOR_TBO = Types.MONITOR_TBO;

    public static final RenderType PRINTOUT_BACKGROUND = Types.PRINTOUT_BACKGROUND;

    public static final RenderType POSITION_COLOR = Types.POSITION_COLOR;

    public static RenderType positionColorTex( ResourceLocation location )
    {
        return Types.POSITION_COLOR_TEX.apply( location );
    }

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
            "monitor_basic", TERM_FORMAT, TERM_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( new ShaderStateShard( RenderTypes::getTerminalShader ) )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_BLOCKER = RenderType.create(
            "monitor_blocker", TERM_FORMAT, TERM_MODE, 256,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( new ShaderStateShard( RenderTypes::getTerminalShader ) )
                .setWriteMaskState( DEPTH_WRITE )
                .setLightmapState( NO_LIGHTMAP )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_WITH_DEPTH = RenderType.create(
            "basic_terminal", TERM_FORMAT, TERM_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( POSITION_COLOR_TEX_SHADER )
                .createCompositeState( false )
        );

        static final RenderType POSITION_COLOR = RenderType.create(
            "position_color", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 128,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setShaderState( POSITION_COLOR_SHADER )
                .createCompositeState( false )
        );

        static final Function<ResourceLocation, RenderType> POSITION_COLOR_TEX = Util.memoize( location ->
        {
            return RenderType.create(
                "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 1024,
                false, false, // useDelegate, needsSorting
                RenderType.CompositeState.builder()
                    .setTextureState( new RenderStateShard.TextureStateShard( location, false, false ) )// blur, minimap
                    .setShaderState( POSITION_COLOR_TEX_SHADER )
                    .createCompositeState( false )
            );
        } );

        static final RenderType PRINTOUT_BACKGROUND = RenderType.create(
            "printout_background", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( new RenderStateShard.TextureStateShard(
                    new ResourceLocation( "computercraft", "textures/gui/printout.png" ),
                    false, false // blur, minimap
                ) )
                .setShaderState( new RenderStateShard.ShaderStateShard( GameRenderer::getPositionTexShader ) )
                .setLightmapState( NO_LIGHTMAP )
                .createCompositeState( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
