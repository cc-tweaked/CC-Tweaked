package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class RenderOverlayCable
{
    private static final float EXPAND = 0.002f;
    private static final double MIN = TileCable.MIN - EXPAND;
    private static final double MAX = TileCable.MAX + EXPAND;

    @SubscribeEvent
    public void drawHighlight( DrawBlockHighlightEvent event )
    {
        if( event.getTarget().typeOfHit != RayTraceResult.Type.BLOCK ) return;

        BlockPos pos = event.getTarget().getBlockPos();
        World world = event.getPlayer().getEntityWorld();

        IBlockState state = world.getBlockState( pos );
        if( state.getBlock() != ComputerCraft.Blocks.cable ) return;

        TileEntity tile = world.getTileEntity( pos );
        if( tile == null || !(tile instanceof TileCable) ) return;

        event.setCanceled( true );
        TileCable cable = (TileCable) tile;

        PeripheralType type = cable.getPeripheralType();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate( GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0 );
        GlStateManager.color( 0.0f, 0.0f, 0.0f, 0.4f );
        GL11.glLineWidth( 2.0F );
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask( false );
        GlStateManager.pushMatrix();

        {
            EntityPlayer player = event.getPlayer();
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

            GlStateManager.translate( -x + pos.getX(), -y + pos.getY(), -z + pos.getZ() );
        }

        if( type != PeripheralType.Cable && WorldUtil.isVecInsideInclusive( cable.getModemBounds(), event.getTarget().hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
        {
            RenderGlobal.drawSelectionBoundingBox( cable.getModemBounds(), 0, 0, 0, 0.4f );
        }
        else
        {
            int flags = 0;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            for( EnumFacing facing : EnumFacing.VALUES )
            {
                if( BlockCable.doesConnectVisually( state, world, pos, facing ) )
                {
                    flags |= 1 << facing.ordinal();


                    switch( facing.getAxis() )
                    {
                        case X:
                        {
                            double offset = facing == EnumFacing.WEST ? -EXPAND : 1 + EXPAND;
                            double centre = facing == EnumFacing.WEST ? MIN : MAX;

                            buffer.begin( GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION );
                            buffer.pos( offset, MIN, MIN ).endVertex();
                            buffer.pos( offset, MAX, MIN ).endVertex();
                            buffer.pos( offset, MAX, MAX ).endVertex();
                            buffer.pos( offset, MIN, MAX ).endVertex();
                            buffer.pos( offset, MIN, MIN ).endVertex();
                            tessellator.draw();

                            buffer.begin( GL11.GL_LINES, DefaultVertexFormats.POSITION );
                            buffer.pos( offset, MIN, MIN ).endVertex();
                            buffer.pos( centre, MIN, MIN ).endVertex();
                            buffer.pos( offset, MAX, MIN ).endVertex();
                            buffer.pos( centre, MAX, MIN ).endVertex();
                            buffer.pos( offset, MAX, MAX ).endVertex();
                            buffer.pos( centre, MAX, MAX ).endVertex();
                            buffer.pos( offset, MIN, MAX ).endVertex();
                            buffer.pos( centre, MIN, MAX ).endVertex();
                            tessellator.draw();
                            break;
                        }
                        case Y:
                        {
                            double offset = facing == EnumFacing.DOWN ? -EXPAND : 1 + EXPAND;
                            double centre = facing == EnumFacing.DOWN ? MIN : MAX;

                            buffer.begin( GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION );
                            buffer.pos( MIN, offset, MIN ).endVertex();
                            buffer.pos( MAX, offset, MIN ).endVertex();
                            buffer.pos( MAX, offset, MAX ).endVertex();
                            buffer.pos( MIN, offset, MAX ).endVertex();
                            buffer.pos( MIN, offset, MIN ).endVertex();
                            tessellator.draw();

                            buffer.begin( GL11.GL_LINES, DefaultVertexFormats.POSITION );
                            buffer.pos( MIN, offset, MIN ).endVertex();
                            buffer.pos( MIN, centre, MIN ).endVertex();
                            buffer.pos( MAX, offset, MIN ).endVertex();
                            buffer.pos( MAX, centre, MIN ).endVertex();
                            buffer.pos( MAX, offset, MAX ).endVertex();
                            buffer.pos( MAX, centre, MAX ).endVertex();
                            buffer.pos( MIN, offset, MAX ).endVertex();
                            buffer.pos( MIN, centre, MAX ).endVertex();
                            tessellator.draw();
                            break;
                        }
                        case Z:
                        {
                            double offset = facing == EnumFacing.NORTH ? -EXPAND : 1 + EXPAND;
                            double centre = facing == EnumFacing.NORTH ? MIN : MAX;

                            buffer.begin( GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION );
                            buffer.pos( MIN, MIN, offset ).endVertex();
                            buffer.pos( MAX, MIN, offset ).endVertex();
                            buffer.pos( MAX, MAX, offset ).endVertex();
                            buffer.pos( MIN, MAX, offset ).endVertex();
                            buffer.pos( MIN, MIN, offset ).endVertex();
                            tessellator.draw();

                            buffer.begin( GL11.GL_LINES, DefaultVertexFormats.POSITION );
                            buffer.pos( MIN, MIN, offset ).endVertex();
                            buffer.pos( MIN, MIN, centre ).endVertex();
                            buffer.pos( MAX, MIN, offset ).endVertex();
                            buffer.pos( MAX, MIN, centre ).endVertex();
                            buffer.pos( MAX, MAX, offset ).endVertex();
                            buffer.pos( MAX, MAX, centre ).endVertex();
                            buffer.pos( MIN, MAX, offset ).endVertex();
                            buffer.pos( MIN, MAX, centre ).endVertex();
                            tessellator.draw();
                            break;
                        }
                    }
                }
            }

            buffer.begin( GL11.GL_LINES, DefaultVertexFormats.POSITION );

            draw( buffer, flags, EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.Axis.Z );
            draw( buffer, flags, EnumFacing.WEST, EnumFacing.UP, EnumFacing.Axis.Z );
            draw( buffer, flags, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.Axis.Z );
            draw( buffer, flags, EnumFacing.EAST, EnumFacing.UP, EnumFacing.Axis.Z );

            draw( buffer, flags, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.Axis.Y );
            draw( buffer, flags, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.Axis.Y );
            draw( buffer, flags, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.Axis.Y );
            draw( buffer, flags, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.Axis.Y );

            draw( buffer, flags, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.Axis.X );
            draw( buffer, flags, EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.Axis.X );
            draw( buffer, flags, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.Axis.X );
            draw( buffer, flags, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.Axis.X );

            tessellator.draw();
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask( true );
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void draw( BufferBuilder buffer, int flags, EnumFacing a, EnumFacing b, EnumFacing.Axis other )
    {
        if( ((flags >> a.ordinal()) & 1) != ((flags >> b.ordinal()) & 1) ) return;

        double offA = a.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? MIN : MAX;
        double offB = b.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE ? MIN : MAX;
        switch( other )
        {
            case X:
                buffer.pos( MIN, offA, offB ).endVertex();
                buffer.pos( MAX, offA, offB ).endVertex();
                break;
            case Y:
                buffer.pos( offA, MIN, offB ).endVertex();
                buffer.pos( offA, MAX, offB ).endVertex();
                break;
            case Z:
                buffer.pos( offA, offB, MIN ).endVertex();
                buffer.pos( offA, offB, MAX ).endVertex();
                break;
        }
    }
}
