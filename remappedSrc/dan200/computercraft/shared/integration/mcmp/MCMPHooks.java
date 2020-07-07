/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.mcmp;

import mcmultipart.MCMultiPart;
import mcmultipart.api.item.ItemBlockMultipart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;

public final class MCMPHooks
{
    private MCMPHooks()
    {
    }

    public static EnumActionResult onItemUse( ItemBlock itemBlock, PlayerEntity player, World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull Direction facing, float hitX, float hitY, float hitZ )
    {
        if( !Loader.isModLoaded( MCMultiPart.MODID ) ) return EnumActionResult.PASS;

        return ItemBlockMultipart.place(
            player, world, pos, hand, facing, hitX, hitY, hitZ, itemBlock,
            itemBlock.getBlock()::getStateForPlacement,
            MCMPIntegration.multipartMap.get( itemBlock.getBlock() ),

            (
                ItemStack stack, PlayerEntity thisPlayer, World thisWorld, BlockPos thisPos, Direction thisFacing,
                float thisX, float thisY, float thisZ, IBlockState thisState
            ) ->
                thisPlayer.canPlayerEdit( thisPos, thisFacing, stack ) &&
                    thisWorld.getBlockState( thisPos ).getBlock().isReplaceable( thisWorld, thisPos ) &&
                    itemBlock.getBlock().canPlaceBlockAt( thisWorld, thisPos ) &&
                    itemBlock.getBlock().canPlaceBlockOnSide( thisWorld, thisPos, thisFacing ) &&
                    itemBlock.placeBlockAt( stack, thisPlayer, thisWorld, thisPos, thisFacing, thisX, thisY, thisZ, thisState ),
            ItemBlockMultipart::placePartAt
        );
    }
}
