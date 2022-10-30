/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.GameTestHolder
import dan200.computercraft.gametest.api.Structures
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.Registry
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.storage.loot.BuiltInLootTables

@GameTestHolder
class Loot_Test {
    /**
     * Test that the loot tables will spawn in treasure disks.
     */
    @GameTest(template = Structures.DEFAULT)
    fun Chest_contains_disk(context: GameTestHelper) = context.sequence {
        thenExecute {
            val pos = BlockPos(2, 2, 2)

            context.setBlock(pos, Blocks.CHEST)
            val chest = context.getBlockEntity(pos) as ChestBlockEntity
            chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 123)
            chest.unpackLootTable(null)

            context.assertContainerContains(pos, Registry.ModItems.TREASURE_DISK.get())
        }
    }
}
