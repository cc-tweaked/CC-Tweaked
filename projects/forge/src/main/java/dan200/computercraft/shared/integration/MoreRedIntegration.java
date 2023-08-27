// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import commoble.morered.api.MoreRedAPI;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.util.SidedCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MoreRedIntegration {
    public static final String MOD_ID = "morered";

    private static final ResourceLocation ID = new ResourceLocation(ComputerCraftAPI.MOD_ID, MOD_ID);

    @SubscribeEvent
    public static void attachBlockCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        var blockEntity = event.getObject();

        if (blockEntity.getBlockState().getBlock() instanceof IBundledRedstoneBlock bundledBlock) {
            // The API is a little unclear on whether this needs to be sided. The API design mirrors Block.getSignal
            // (suggesting we can use wireFace.getOpposite(), which is what we did on older versions), but on the other
            // hand that parameter is not guaranteed to be non-null (suggesting we should use the cap side instead).
            SidedCapabilityProvider.attach(event, ID, MoreRedAPI.CHANNELED_POWER_CAPABILITY, side -> (world, wirePos, wireState, wireFace, channel) -> {
                if (side == null) return 0; // It's not clear if there's a sensible implementation here.

                var level = bundledBlock.getBundledRedstoneOutput(world, blockEntity.getBlockPos(), side);
                return (level & (1 << channel)) != 0 ? 31 : 0;
            });
        }
    }

    public static void setup() {
        MinecraftForge.EVENT_BUS.register(MoreRedIntegration.class);
        ComputerCraftAPI.registerBundledRedstoneProvider(MoreRedIntegration::getBundledPower);
    }

    private static int getBundledPower(Level world, BlockPos pos, Direction side) {
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) return -1;

        var blockState = blockEntity.getBlockState();

        // Skip ones already handled by CC. We can do this more efficiently.
        if (blockState.getBlock() instanceof IBundledRedstoneBlock) return -1;

        var powerCap = blockEntity.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, side);
        if (!powerCap.isPresent()) return -1;
        var power = powerCap.orElseThrow(NullPointerException::new);

        var mask = 0;
        for (var i = 0; i < 16; i++) {
            mask |= power.getPowerOnChannel(world, pos, blockState, side, i) > 0 ? (1 << i) : 0;
        }
        return mask;
    }
}
