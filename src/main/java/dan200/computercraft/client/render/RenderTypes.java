/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;


public class RenderTypes
{
    public static final ResourceLocation FONT = new ResourceLocation( "computercraft", "textures/gui/term_font.png" );
    public static final ResourceLocation PRINTOUT_BACKGROUND = new ResourceLocation( "computercraft", "textures/gui/printout.png" );
    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

    public static final RenderType MONITOR_TBO = Types.MONITOR_TBO;
    public static final RenderType MONITOR = RenderType.textIntensity( FONT );

    public static final RenderType ITEM_POCKET_TERMINAL = RenderType.textIntensity( FONT );
    public static final RenderType ITEM_POCKET_LIGHT = RenderType.textIntensity( FONT );
    public static final RenderType ITEM_PRINTOUT_BACKGROUND = RenderType.entityCutout( PRINTOUT_BACKGROUND );
    public static final RenderType ITEM_PRINTOUT_TEXT = RenderType.entityCutout( FONT );

    public static final RenderType GUI_TERMINAL = RenderType.text( FONT );
    public static final RenderType GUI_PRINTOUT_BACKGROUND = RenderType.text( PRINTOUT_BACKGROUND );
    public static final RenderType GUI_PRINTOUT_TEXT = RenderType.text( FONT );

    public static ShaderInstance getMonitorShader()
    {
        return GameRenderer.getRendertypeTextIntensityShader();
    }

    public static RenderType itemPocketBorder( ResourceLocation location )
    {
        return RenderType.entityCutout( location );
    }

    public static RenderType guiComputerBorder( ResourceLocation location )
    {
        return RenderType.text( location );
    }

    public static MonitorTextureBufferShader monitorTboShader;

    @Nonnull
    static MonitorTextureBufferShader getMonitorTextureBufferShader()
    {
        if( monitorTboShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return monitorTboShader;
    }

    private static final class Types extends RenderStateShard
    {
        private static final RenderStateShard.TextureStateShard TERM_FONT_TEXTURE = new TextureStateShard(
            FONT,
            false, false // blur, minimap
        );

        static final RenderType MONITOR_TBO = RenderType.create(
            "monitor_tbo", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLE_STRIP, 128,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( new ShaderStateShard( RenderTypes::getMonitorTextureBufferShader ) )
                .createCompositeState( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
