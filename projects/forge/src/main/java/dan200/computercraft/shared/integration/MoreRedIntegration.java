// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import commoble.morered.api.MoreRedAPI;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class MoreRedIntegration {
    public static final String MOD_ID = "morered";

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof IBundledRedstoneBlock bundledBlock)) continue;
            event.registerBlock(
                MoreRedAPI.CHANNELED_POWER_CAPABILITY,
                (level, pos, state, blockEntity, context) -> (level2, wirePos, wireState, wireFace, channel) -> {
                    var outputLevel = bundledBlock.getBundledRedstoneOutput(level2, pos, context);
                    return (outputLevel & (1 << channel)) != 0 ? 31 : 0;
                },
                block
            );
        }
    }

    public static void setup(IEventBus bus) {
        bus.addListener(MoreRedIntegration::onRegisterCapabilities);
        ComputerCraftAPI.registerBundledRedstoneProvider(MoreRedIntegration::getBundledPower);
    }

    private static int getBundledPower(Level world, BlockPos pos, Direction side) {
        var blockState = world.getBlockState(pos);

        // Skip ones already handled by CC. We can do this more efficiently.
        if (blockState.getBlock() instanceof IBundledRedstoneBlock) return -1;

        var power = world.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, pos, blockState, null, side);
        if (power == null) return -1;

        var mask = 0;
        for (var i = 0; i < 16; i++) {
            mask |= power.getPowerOnChannel(world, pos, blockState, side, i) > 0 ? (1 << i) : 0;
        }
        return mask;
    }
}
