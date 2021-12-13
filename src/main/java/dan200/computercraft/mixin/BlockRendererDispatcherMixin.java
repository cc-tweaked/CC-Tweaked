/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockDisplayReader;
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
 * @see BlockRendererDispatcher#renderBlockDamage(BlockState, BlockPos, IBlockDisplayReader, MatrixStack, IVertexBuilder, IModelData)
 */
@Mixin( BlockRendererDispatcher.class )
public class BlockRendererDispatcherMixin
{
    @Shadow
    private final Random random;
    @Shadow
    private final BlockModelShapes blockModelShaper;
    @Shadow
    private final BlockModelRenderer modelRenderer;

    public BlockRendererDispatcherMixin( Random random, BlockModelShapes blockModelShaper, BlockModelRenderer modelRenderer )
    {
        this.random = random;
        this.blockModelShaper = blockModelShaper;
        this.modelRenderer = modelRenderer;
    }

    @Inject(
        method = "name=/^renderBlockDamage$/ desc=/IModelData;\\)V$/",
        at = @At( "HEAD" ),
        cancellable = true,
        require = 0 // This isn't critical functionality, so don't worry if we can't apply it.
    )
    public void renderBlockDamage(
        BlockState state, BlockPos pos, IBlockDisplayReader world, MatrixStack pose, IVertexBuilder buffers, IModelData modelData,
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

        RayTraceResult hit = Minecraft.getInstance().hitResult;
        if( hit == null || hit.getType() != RayTraceResult.Type.BLOCK ) return;
        BlockPos hitPos = ((BlockRayTraceResult) hit).getBlockPos();

        if( !hitPos.equals( pos ) ) return;

        info.cancel();
        BlockState newState = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getLocation().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? state.getBlock().defaultBlockState().setValue( BlockCable.MODEM, state.getValue( BlockCable.MODEM ) )
            : state.setValue( BlockCable.MODEM, CableModemVariant.None );

        IBakedModel model = blockModelShaper.getBlockModel( newState );
        long seed = newState.getSeed( pos );
        modelRenderer.renderModel( world, model, newState, pos, pose, buffers, true, random, seed, OverlayTexture.NO_OVERLAY, modelData );
    }
}
