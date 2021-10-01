package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RenderTypes {

    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

    @Nullable
    public static MonitorTextureBufferShader monitorTboShader;

    @Nullable
    public static Shader terminalShader;

    public static final RenderLayer TERMINAL_WITHOUT_DEPTH = Types.TERMINAL_WITHOUT_DEPTH;
    public static final RenderLayer MONITOR_TBO = Types.MONITOR_TBO;
    public static final RenderLayer TERMINAL_BLOCKER = Types.BLOCKER;
    public static final RenderLayer TERMINAL_WITH_DEPTH = Types.TERMINAL_WITH_DEPTH;
    public static final RenderLayer PRINTOUT_TEXT = Types.PRINTOUT_TEXT;

    public static final RenderLayer PRINTOUT_BACKGROUND = RenderLayer.getText(new Identifier( "computercraft", "textures/gui/printout.png" ));

    public static final RenderLayer POSITION_COLOR = Types.POSITION_COLOR;

    @Nonnull
    static MonitorTextureBufferShader getMonitorTextureBufferShader()
    {
        if( monitorTboShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return monitorTboShader;
    }

    @Nonnull
    static Shader getTerminalShader()
    {
        if( terminalShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return terminalShader;
    }

    private static final class Types extends RenderPhase
    {
        private static final VertexFormat.DrawMode GL_MODE = VertexFormat.DrawMode.TRIANGLES;
        private static final VertexFormat FORMAT = VertexFormats.POSITION_COLOR_TEXTURE;
        private static final Shader TERM_SHADER = new Shader( RenderTypes::getTerminalShader );

        private static final RenderPhase.Texture TERM_FONT_TEXTURE = new RenderPhase.Texture(
            FixedWidthFontRenderer.FONT,
            false, false // blur, minimap
        );

        public static final RenderLayer MONITOR_TBO = RenderLayer.of( "monitor_tbo", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLE_STRIP, 128, false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture(TERM_FONT_TEXTURE ) // blur, minimap
                .shader(new RenderPhase.Shader(RenderTypes::getMonitorTextureBufferShader))
                .writeMaskState( RenderLayer.ALL_MASK )
                .build( false ) );

        static final RenderLayer TERMINAL_WITHOUT_DEPTH = RenderLayer.of(
            "terminal_without_depth", FORMAT, GL_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture( TERM_FONT_TEXTURE )
                .shader( TERM_SHADER )
                .writeMaskState( COLOR_MASK )
                .build( false )
        );

        static final RenderLayer BLOCKER = RenderLayer.of( "terminal_blocker", FORMAT, GL_MODE, 256, false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture( TERM_FONT_TEXTURE )
                .shader(TERM_SHADER)
                .writeMaskState( DEPTH_MASK )
                .build( false ) );

        static final RenderLayer TERMINAL_WITH_DEPTH = RenderLayer.of(
            "terminal_with_depth", FORMAT, GL_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture( TERM_FONT_TEXTURE )
                .shader( TERM_SHADER )
                .build( false )
        );

        static final RenderLayer PRINTOUT_TEXT = RenderLayer.of(
            "printout_text", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, GL_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture( TERM_FONT_TEXTURE )
                .shader( RenderPhase.TEXT_SHADER )
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .build( false )
        );

        static final RenderLayer POSITION_COLOR = RenderLayer.of(
            "position_color", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 128,
            false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .shader( COLOR_SHADER )
                .build( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
