// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.items;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.shared.computer.blocks.ComputerBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link ComputerItem} which prevents players placing it without permission.
 *
 * @see net.minecraft.world.item.GameMasterBlockItem
 * @see dan200.computercraft.shared.computer.blocks.CommandComputerBlock
 */
public class CommandComputerItem extends ComputerItem {
    public CommandComputerItem(ComputerBlock<?> block, Properties settings) {
        super(block, settings);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        // Prohibit players placing this block in survival or when not opped.
        var player = context.getPlayer();
        return player != null && !player.canUseGameMasterBlocks() ? null : super.getPlacementState(context);
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        // Don't allow command computers to be mounted in disk drives.
        return null;
    }
}
