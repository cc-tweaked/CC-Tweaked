/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.core.apis.RedstoneAPI
import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.gametest.api.*
import dan200.computercraft.test.core.computer.getApi
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.RedstoneLampBlock

@GameTestHolder
class Computer_Test {
    /**
     * Ensures redstone signals do not travel through computers.
     *
     * @see [#548](https://github.com/cc-tweaked/CC-Tweaked/issues/548)
     */
    @GameTest
    fun No_through_signal(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 4)
        val lever = BlockPos(2, 2, 0)
        thenExecute {
            context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit")
            context.modifyBlock(lever) { x -> x.setValue(LeverBlock.POWERED, true) }
        }
        thenIdle(3)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should still not be lit") }
    }

    /**
     * Similar to the above, but with a repeater before the computer
     */
    @GameTest
    fun No_through_signal_reverse(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 4)
        val lever = BlockPos(2, 2, 0)
        thenExecute {
            context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit")
            context.modifyBlock(lever) { x -> x.setValue(LeverBlock.POWERED, true) }
        }
        thenIdle(3)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should still not be lit") }
    }

    @GameTest
    fun Set_and_destroy(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 3)

        thenOnComputer { getApi<RedstoneAPI>().setOutput(ComputerSide.BACK, true) }
        thenIdle(3)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, true, "Lamp should be lit") }
        thenExecute { context.setBlock(BlockPos(2, 2, 2), Blocks.AIR) }
        thenIdle(4)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit") }
    }

    @GameTest
    fun Computer_peripheral(context: GameTestHelper) = context.sequence {
        thenExecute {
            context.assertPeripheral(BlockPos(3, 2, 2), type = "computer")
            context.assertPeripheral(BlockPos(1, 2, 2), type = "turtle")
        }
    }
}
