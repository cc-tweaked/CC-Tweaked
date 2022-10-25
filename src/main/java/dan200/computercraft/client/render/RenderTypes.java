/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderTypes
{
    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

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
     * <p>
     * This is identical to <em>vanilla's</em> {@link RenderType#text}. Forge overrides one with a definition which sets
     * sortOnUpload to true, which is entirely broken!
     */
    public static final RenderType TERMINAL_WITH_DEPTH = Types.TERMINAL_WITH_DEPTH;

    /**
     * A variant of {@link #TERMINAL_WITH_DEPTH} which uses the lightmap rather than rendering fullbright.
     */
    public static final RenderType PRINTOUT_TEXT = RenderType.text( FixedWidthFontRenderer.FONT );

    /**
     * Printout's background texture. {@link RenderType#text(ResourceLocation)} is a <em>little</em> questionable, but
     * it is what maps use, so should behave the same as vanilla in both item frames and in-hand.
     */
    public static final RenderType PRINTOUT_BACKGROUND = RenderType.text( new ResourceLocation( "computercraft", "textures/gui/printout.png" ) );

    private static final class Types extends RenderState
    {
        private static final RenderState.TextureState TERM_FONT_TEXTURE = new RenderState.TextureState(
            FixedWidthFontRenderer.FONT,
            false, false // blur, minimap
        );
        private static final VertexFormat TERM_FORMAT = DefaultVertexFormats.POSITION_COLOR_TEX;

        static final RenderType TERMINAL_WITHOUT_DEPTH = RenderType.create(
            "terminal_without_depth", TERM_FORMAT, GL11.GL_QUADS, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.State.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setAlphaState( DEFAULT_ALPHA )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_BLOCKER = RenderType.create(
            "terminal_blocker", DefaultVertexFormats.POSITION, GL11.GL_QUADS, 256,
            false, false, // useDelegate, needsSorting
            RenderType.State.builder()
                .setWriteMaskState( DEPTH_WRITE )
                .createCompositeState( false )
        );

        static final RenderType TERMINAL_WITH_DEPTH = RenderType.create(
            "terminal_with_depth", TERM_FORMAT, GL11.GL_QUADS, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.State.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setAlphaState( DEFAULT_ALPHA )
                .createCompositeState( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
