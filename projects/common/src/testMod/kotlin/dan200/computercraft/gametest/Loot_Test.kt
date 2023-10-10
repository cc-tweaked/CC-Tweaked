// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.ModRegistry
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.storage.loot.BuiltInLootTables

class Loot_Test {
    /**
     * Test that the loot tables will spawn in treasure disks.
     */
    @GameTest(template = Structures.DEFAULT, required = false) // FIXME: We may need to inject this as a datapack instead
    fun Chest_contains_disk(context: GameTestHelper) = context.sequence {
        thenExecute {
            val pos = BlockPos(2, 2, 2)

            context.setBlock(pos, Blocks.CHEST)
            val chest = context.getBlockEntity(pos) as ChestBlockEntity
            chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 123)
            chest.unpackLootTable(null)

            context.assertContainerContains(pos, ModRegistry.Items.TREASURE_DISK.get())
        }
    }
}
