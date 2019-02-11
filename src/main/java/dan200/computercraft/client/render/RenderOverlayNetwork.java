package dan200.computercraft.client.render;

import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.shared.wired.WiredNetwork;
import dan200.computercraft.shared.wired.WiredNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collection;
import java.util.Set;

/**
 * This is a helper to render a network when testing.
 */
public final class RenderOverlayNetwork
{
    private int ticksInGame;
    private IWiredElement element;

    @SubscribeEvent
    public void onWorldRenderLast( RenderWorldLastEvent event )
    {
        ++ticksInGame;

        RayTraceResult result = Minecraft.getMinecraft().objectMouseOver;
        if( result != null && result.typeOfHit == RayTraceResult.Type.BLOCK )
        {
            World clientWorld = Minecraft.getMinecraft().world;
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld( clientWorld.provider.getDimension() );

            IWiredElement newElement = ComputerCraft.getWiredElementAt( world, result.getBlockPos(), result.sideHit );
            if( newElement != null ) element = newElement;
        }

        if( element == null ) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        ItemStack stack = minecraft.player.getHeldItemMainhand();
        ItemStack otherStack = minecraft.player.getHeldItemOffhand();

        if( stack.getItem() != Items.STICK && otherStack.getItem() != Items.STICK ) return;

        GlStateManager.pushMatrix();
        RenderManager renderManager = minecraft.getRenderManager();
        GlStateManager.translate( -renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ );

        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA );

        Set<Pair<IWiredElement>> connections = Sets.newHashSet();
        WiredNetwork network = (WiredNetwork) element.getNode().getNetwork();

        for( WiredNode node : network.getNodes() )
        {
            for( WiredNode other : node.getNeighbours() )
            {
                connections.add( new Pair<>( node.getElement(), other.getElement() ) );
            }
        }

        renderNetworkConnections( connections, new Color( Color.HSBtoRGB( ticksInGame % 200 / 200F, 0.6F, 1F ) ), 1f );

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderNetworkConnections( Collection<Pair<IWiredElement>> data, Color color, float thickness )
    {
        renderConnections( data, color, 1.0f, thickness );
        renderConnections( data, color, 64.0f / 255.0f, thickness * 3 );
    }

    private void renderConnections( Collection<Pair<IWiredElement>> connections, Color color, float alpha, float thickness )
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.scale( 1, 1, 1 );

        GlStateManager.color( color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, alpha );
        GL11.glLineWidth( thickness );

        renderer.begin( GL11.GL_LINES, DefaultVertexFormats.POSITION );
        for( Pair<IWiredElement> connection : connections )
        {
            Vec3d a = connection.x.getPosition(), b = connection.y.getPosition();

            renderer.pos( a.x, a.y, a.z ).endVertex();
            renderer.pos( b.x, b.y, b.z ).endVertex();
        }

        tessellator.draw();

        GlStateManager.popMatrix();
    }

    private void renderLabel( double x, double y, double z, String label )
    {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        FontRenderer fontrenderer = renderManager.getFontRenderer();
        if( fontrenderer == null ) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();

        float scale = 0.02666667f;
        GlStateManager.translate( x, y, z );
        GlStateManager.rotate( -renderManager.playerViewY, 0, 1, 0 );
        GlStateManager.rotate( renderManager.playerViewX, 1, 0, 0 );
        GlStateManager.scale( -scale, -scale, scale );

        // Render label background
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();

        int width = fontrenderer.getStringWidth( label );
        int xOffset = width / 2;

        GlStateManager.disableTexture2D();
        GlStateManager.color( 0, 0, 0, 65 / 225.0f );

        renderer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION );
        renderer.pos( -xOffset - 1, -1, 0 ).endVertex();
        renderer.pos( -xOffset - 1, 8, 0 ).endVertex();
        renderer.pos( xOffset + 1, 8, 0 ).endVertex();
        renderer.pos( xOffset + 1, -1, 0 ).endVertex();

        tessellator.draw();
        GlStateManager.enableTexture2D();

        // Render label
        fontrenderer.drawString( label, -width / 2, 0, 0xFFFFFFFF );

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static class Pair<T>
    {
        public final T x;
        public final T y;

        public Pair( T right, T y )
        {
            this.x = right;
            this.y = y;
        }

        @Override
        public boolean equals( Object o )
        {
            if( this == o ) return true;
            if( o == null || getClass() != o.getClass() ) return false;

            Pair<?> p = (Pair<?>) o;
            return (x.equals( p.x ) && y.equals( p.y ))
                || (x.equals( p.y ) && y.equals( p.x ));
        }

        @Override
        public int hashCode()
        {
            return x.hashCode() ^ y.hashCode();
        }
    }
}
