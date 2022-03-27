/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.*;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

public class TileEntityMonitorRenderer implements BlockEntityRenderer<TileMonitor>
{
    /**
     * {@link TileMonitor#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.05);
    private static ByteBuffer tboContents;

    private static final Matrix3f IDENTITY;

    static
    {
        Matrix3f identity = new Matrix3f();
        identity.setIdentity();
        IDENTITY = identity;
    }

    public TileEntityMonitorRenderer( BlockEntityRendererProvider.Context context )
    {
    }

    @Override
    public void render( @Nonnull TileMonitor monitor, float partialTicks, @Nonnull PoseStack transform, @Nonnull MultiBufferSource renderer, int lightmapCoord, int overlayLight )
    {
        // Render from the origin monitor
        ClientMonitor originTerminal = monitor.getClientMonitor();

        if( originTerminal == null ) return;
        TileMonitor origin = originTerminal.getOrigin();
        BlockPos monitorPos = monitor.getBlockPos();

        // Ensure each monitor terminal is rendered only once. We allow rendering a specific tile
        // multiple times in a single frame to ensure compatibility with shaders which may run a
        // pass multiple times.
        long renderFrame = FrameInfo.getRenderFrame();
        if( originTerminal.lastRenderFrame == renderFrame && !monitorPos.equals( originTerminal.lastRenderPos ) )
        {
            return;
        }

        originTerminal.lastRenderFrame = renderFrame;
        originTerminal.lastRenderPos = monitorPos;

        BlockPos originPos = origin.getBlockPos();

        // Determine orientation
        Direction dir = origin.getDirection();
        Direction front = origin.getFront();
        float yaw = dir.toYRot();
        float pitch = DirectionUtil.toPitchAngle( front );

        // Setup initial transform
        transform.pushPose();
        transform.translate(
            originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5
        );

        transform.mulPose( Vector3f.YN.rotationDegrees( yaw ) );
        transform.mulPose( Vector3f.XP.rotationDegrees( pitch ) );
        transform.translate(
            -0.5 + TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN,
            origin.getHeight() - 0.5 - (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN) + 0,
            0.5
        );
        double xSize = origin.getWidth() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);
        double ySize = origin.getHeight() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);

        // Draw the contents
        Terminal terminal = originTerminal.getTerminal();
        if( terminal != null )
        {
            // Draw a terminal
            int width = terminal.getWidth(), height = terminal.getHeight();
            int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
            double xScale = xSize / pixelWidth;
            double yScale = ySize / pixelHeight;
            transform.pushPose();
            // Avoid PoseStack#scale to preserve normal matrix.
            transform.last().pose().multiply( Matrix4f.createScaleMatrix( (float) xScale, (float) -yScale, 1.0f ) );

            renderTerminal( renderer, transform, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            // We don't draw the cursor with the VBO, as it's dynamic and so we'll end up refreshing far more than is
            // reasonable.
            FixedWidthFontRenderer.drawCursor(
                transform, renderer.getBuffer( RenderTypes.MONITOR ),
                0, 0, terminal, !originTerminal.isColour()
            );

            transform.popPose();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                transform, renderer.getBuffer( RenderTypes.MONITOR ),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2), FULL_BRIGHT_LIGHTMAP
            );
        }

        transform.popPose();
    }

    private static void renderTerminal( @Nonnull MultiBufferSource renderer, PoseStack transform, ClientMonitor monitor, float xMargin, float yMargin )
    {
        Terminal terminal = monitor.getTerminal();

        MonitorRenderer renderType = MonitorRenderer.current();
        boolean redraw = monitor.pollTerminalChanged();
        if( monitor.createBuffer( renderType ) ) redraw = true;

        switch( renderType )
        {
            case TBO:
            {
                int width = terminal.getWidth(), height = terminal.getHeight();

                int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
                if( redraw )
                {
                    int size = width * height * 3;
                    if( tboContents == null || tboContents.capacity() < size )
                    {
                        tboContents = MemoryTracker.create( size );
                    }

                    ByteBuffer monitorBuffer = tboContents;
                    monitorBuffer.clear();
                    for( int y = 0; y < height; y++ )
                    {
                        TextBuffer text = terminal.getLine( y ), textColour = terminal.getTextColourLine( y ), background = terminal.getBackgroundColourLine( y );
                        for( int x = 0; x < width; x++ )
                        {
                            monitorBuffer.put( (byte) (text.charAt( x ) & 0xFF) );
                            monitorBuffer.put( (byte) getColour( textColour.charAt( x ), Colour.WHITE ) );
                            monitorBuffer.put( (byte) getColour( background.charAt( x ), Colour.BLACK ) );
                        }
                    }
                    monitorBuffer.flip();

                    GlStateManager._glBindBuffer( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer );
                    GlStateManager._glBufferData( GL31.GL_TEXTURE_BUFFER, monitorBuffer, GL20.GL_STATIC_DRAW );
                    GlStateManager._glBindBuffer( GL31.GL_TEXTURE_BUFFER, 0 );
                }

                // Nobody knows what they're doing!
                int active = GlStateManager._getActiveTexture();
                RenderSystem.activeTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                RenderSystem.activeTexture( active );

                MonitorTextureBufferShader shader = RenderTypes.getMonitorTextureBufferShader();
                shader.setupUniform( width, height, terminal.getPalette(), !monitor.isColour() );

                Matrix4f matrix = transform.last().pose();
                VertexConsumer buffer = renderer.getBuffer( RenderTypes.MONITOR_TBO );
                tboVertex( buffer, matrix, -xMargin, -yMargin );
                tboVertex( buffer, matrix, -xMargin, pixelHeight + yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, -yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, pixelHeight + yMargin );

                break;
            }

            case VBO:
            {
                VertexBuffer vbo = monitor.buffer;
                if( redraw )
                {
                    Tesselator tessellator = Tesselator.getInstance();
                    BufferBuilder builder = tessellator.getBuilder();
                    builder.begin( RenderTypes.MONITOR.mode(), RenderTypes.MONITOR.format() );

                    FixedWidthFontRenderer.drawTerminalWithoutCursor(
                        new PoseStack(), builder, 0, 0,
                        terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin, FULL_BRIGHT_LIGHTMAP
                    );

                    builder.end();
                    vbo.upload( builder );
                }

                Matrix3f popViewRotation = RenderSystem.getInverseViewRotationMatrix();
                RenderSystem.setInverseViewRotationMatrix( IDENTITY );

                RenderTypes.MONITOR.setupRenderState();
                vbo.drawWithShader( transform.last().pose(), RenderSystem.getProjectionMatrix(), RenderTypes.getMonitorShader() );
                RenderTypes.MONITOR.clearRenderState();

                RenderSystem.setInverseViewRotationMatrix( popViewRotation );

                // How we would draw the terminal without using VBO, for reference until I need to delete this.
                //VertexConsumer buffer = renderer.getBuffer( RenderTypes.MONITOR );
                //FixedWidthFontRenderer.drawTerminalWithoutCursor(
                //    transform, buffer, 0, 0,
                //    terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin, FULL_BRIGHT_LIGHTMAP
                //);
                break;
            }
        }
    }

    private static void tboVertex( VertexConsumer builder, Matrix4f matrix, float x, float y )
    {
        // We encode position in the UV, as that's not transformed by the matrix.
        builder.vertex( matrix, x, y, 0 ).uv( x, y ).endVertex();
    }

    @Override
    public int getViewDistance()
    {
        return ComputerCraft.monitorDistance;
    }
}
