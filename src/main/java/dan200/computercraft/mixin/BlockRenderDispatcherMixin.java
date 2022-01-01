/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Provides custom block breaking progress for modems, so it only applies to the current part.
 *
 * @see BlockRenderDispatcher#renderBreakingTexture(BlockState, BlockPos, BlockAndTintGetter, PoseStack, VertexConsumer, IModelData)
 */
@Mixin( BlockRenderDispatcher.class )
public class BlockRenderDispatcherMixin
{
    @Shadow
    private final Random random;
    @Shadow
    private final BlockModelShaper blockModelShaper;
    @Shadow
    private final ModelBlockRenderer modelRenderer;

    public BlockRenderDispatcherMixin( Random random, BlockModelShaper blockModelShaper, ModelBlockRenderer modelRenderer )
    {
        this.random = random;
        this.blockModelShaper = blockModelShaper;
        this.modelRenderer = modelRenderer;
    }

    @Inject(
        method = "name=/^renderBreakingTexture/ desc=/IModelData;\\)V$/",
        at = @At( "HEAD" ),
        cancellable = true,
        require = 0 // This isn't critical functionality, so don't worry if we can't apply it.
    )
    public void renderBlockDamage(
        BlockState state, BlockPos pos, BlockAndTintGetter world, PoseStack pose, VertexConsumer buffers, IModelData modelData,
        CallbackInfo info
    )
    {
        // Only apply to cables which have both a cable and modem
        if( state.getBlock() != Registry.ModBlocks.CABLE.get()
            || !state.getValue( BlockCable.CABLE )
            || state.getValue( BlockCable.MODEM ) == CableModemVariant.None
        )
        {
            return;
        }

        HitResult hit = Minecraft.getInstance().hitResult;
        if( hit == null || hit.getType() != HitResult.Type.BLOCK ) return;
        BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();

        if( !hitPos.equals( pos ) ) return;

        info.cancel();
        BlockState newState = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getLocation().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? state.getBlock().defaultBlockState().setValue( BlockCable.MODEM, state.getValue( BlockCable.MODEM ) )
            : state.setValue( BlockCable.MODEM, CableModemVariant.None );

        BakedModel model = blockModelShaper.getBlockModel( newState );
        long seed = newState.getSeed( pos );
        modelRenderer.tesselateBlock( world, model, newState, pos, pose, buffers, true, random, seed, OverlayTexture.NO_OVERLAY, modelData );
    }
}
