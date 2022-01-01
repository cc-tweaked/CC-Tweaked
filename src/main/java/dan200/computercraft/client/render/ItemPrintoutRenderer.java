/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINES_PER_PAGE;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINE_MAX_LENGTH;

/**
 * Emulates map and item-frame rendering for printouts.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class ItemPrintoutRenderer extends ItemMapLikeRenderer
{
    private static final ItemPrintoutRenderer INSTANCE = new ItemPrintoutRenderer();

    private ItemPrintoutRenderer()
    {
    }

    @SubscribeEvent
    public static void onRenderInHand( RenderHandEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( !(stack.getItem() instanceof ItemPrintout) ) return;

        event.setCanceled( true );
        INSTANCE.renderItemFirstPerson(
            event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
            event.getHand(), event.getInterpolatedPitch(), event.getEquipProgress(), event.getSwingProgress(), event.getItemStack()
        );
    }

    @Override
    protected void renderItem( PoseStack transform, MultiBufferSource render, ItemStack stack, int light )
    {
        transform.mulPose( Vector3f.XP.rotationDegrees( 180f ) );
        transform.scale( 0.42f, 0.42f, -0.42f );
        transform.translate( -0.5f, -0.48f, 0.0f );

        drawPrintout( transform, render, stack, light );
    }

    @SubscribeEvent
    public static void onRenderInFrame( RenderItemInFrameEvent event )
    {
        ItemStack stack = event.getItemStack();
        if( !(stack.getItem() instanceof ItemPrintout) ) return;
        event.setCanceled( true );

        PoseStack transform = event.getPoseStack();

        // Move a little bit forward to ensure we're not clipping with the frame
        transform.translate( 0.0f, 0.0f, -0.001f );
        transform.mulPose( Vector3f.ZP.rotationDegrees( 180f ) );
        transform.scale( 0.95f, 0.95f, -0.95f );
        transform.translate( -0.5f, -0.5f, 0.0f );

        int light = event.getItemFrameEntity().getType() == EntityType.GLOW_ITEM_FRAME ? 0xf000d2 : event.getPackedLight(); // See getLightVal.
        drawPrintout( transform, event.getMultiBufferSource(), stack, light );
    }

    private static void drawPrintout( PoseStack transform, MultiBufferSource render, ItemStack stack, int light )
    {
        int pages = ItemPrintout.getPageCount( stack );
        boolean book = ((ItemPrintout) stack.getItem()).getType() == ItemPrintout.Type.BOOK;

        double width = LINE_MAX_LENGTH * FONT_WIDTH + X_TEXT_MARGIN * 2;
        double height = LINES_PER_PAGE * FONT_HEIGHT + Y_TEXT_MARGIN * 2;

        // Non-books will be left aligned
        if( !book ) width += offsetAt( pages );

        double visualWidth = width, visualHeight = height;

        // Meanwhile books will be centred
        if( book )
        {
            visualWidth += 2 * COVER_SIZE + 2 * offsetAt( pages );
            visualHeight += 2 * COVER_SIZE;
        }

        double max = Math.max( visualHeight, visualWidth );

        // Scale the printout to fit correctly.
        float scale = (float) (1.0 / max);
        transform.scale( scale, scale, scale );
        transform.translate( (max - width) / 2.0, (max - height) / 2.0, 0.0 );

        Matrix4f matrix = transform.last().pose();
        drawBorder( matrix, render, 0, 0, -0.01f, 0, pages, book, light );
        drawText(
            matrix, render, X_TEXT_MARGIN, Y_TEXT_MARGIN, 0, light,
            ItemPrintout.getText( stack ), ItemPrintout.getColours( stack )
        );
    }
}
