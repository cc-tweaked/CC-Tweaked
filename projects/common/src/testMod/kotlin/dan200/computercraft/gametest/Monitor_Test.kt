/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.peripheral.monitor.MonitorBlock
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import java.util.*

@GameTestHolder
class Monitor_Test {
    @GameTest
    fun Ensures_valid_on_place(context: GameTestHelper) = context.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val tag = CompoundTag()
            tag.putInt("Width", 2)
            tag.putInt("Height", 2)

            val toSet = BlockInput(
                ModRegistry.Blocks.MONITOR_ADVANCED.get().defaultBlockState(),
                Collections.emptySet(),
                tag,
            )

            context.setBlock(pos, Blocks.AIR.defaultBlockState())
            context.setBlock(pos, toSet)
        }
        thenIdle(2)
        thenExecute {
            val tile = context.getBlockEntity(pos, ModRegistry.BlockEntities.MONITOR_ADVANCED.get())

            if (tile.width != 1 || tile.height != 1) {
                context.fail("Tile has width and height of ${tile.width}x${tile.height}, but should be 1x1", pos)
            }
        }
    }

    /**
     * When a monitor is destroyed, assert its neighbors correctly contract.
     */
    @GameTest
    fun Contract_on_destroy(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.setBlock(BlockPos(2, 2, 2), Blocks.AIR.defaultBlockState())
            helper.assertBlockHas(BlockPos(1, 2, 2), MonitorBlock.STATE, MonitorEdgeState.NONE)
            helper.assertBlockHas(BlockPos(3, 2, 2), MonitorBlock.STATE, MonitorEdgeState.NONE)
        }
    }
}
