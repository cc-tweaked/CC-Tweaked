/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.ComputerCraftRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin( ServerPlayerGameMode.class )
public class MixinServerPlayerInteractionManager
{
    @Inject( at = @At( value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;", ordinal = 0 ), method = "interactBlock(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", cancellable = true )
    private void interact( ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir )
    {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState( pos );
        if( player.getMainHandItem().getItem() == ComputerCraftRegistry.ModItems.DISK && state.getBlock() == ComputerCraftRegistry.ModBlocks.DISK_DRIVE )
        {
            InteractionResult actionResult = state.use( world, player, hand, hitResult );
            if( actionResult.consumesAction() )
            {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger( player, pos, stack );
                cir.setReturnValue( actionResult );
            }
        }
    }
}
