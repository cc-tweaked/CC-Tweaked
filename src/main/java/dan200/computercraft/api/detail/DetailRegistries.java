/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import dan200.computercraft.impl.ComputerCraftAPIService;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;

/**
 * {@link DetailRegistry}s for built-in Minecraft types.
 */
public class DetailRegistries
{
    /**
     * Provides details for {@link ItemStack}s.
     */
    public static final DetailRegistry<ItemStack> ITEM_STACK = ComputerCraftAPIService.get().getItemStackDetailRegistry();

    /**
     * Provides details for {@link BlockReference}, a reference to a {@link Block} in the world.
     */
    public static final DetailRegistry<BlockReference> BLOCK_IN_WORLD = ComputerCraftAPIService.get().getBlockInWorldDetailRegistry();

    /**
     * Provides details for {@link FluidStack}.
     */
    public static final DetailRegistry<FluidStack> FLUID_STACK = ComputerCraftAPIService.get().getFluidStackDetailRegistry();
}
