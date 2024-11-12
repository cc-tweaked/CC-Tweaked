// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.core.computer.ComputerSide
import dan200.computercraft.gametest.api.assertBlockHas
import dan200.computercraft.gametest.api.getBlockEntity
import dan200.computercraft.gametest.api.modifyBlock
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.peripheral.redstone.RedstoneRelayPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.RedstoneLampBlock
import org.junit.jupiter.api.Assertions.assertEquals

class Relay_Test {
    /**
     * Ensures redstone signals do not travel through relay.
     *
     * @see [Computer_Test.No_through_signal]
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
     * Similar to the above, but with a repeater before the relay
     *
     * @see [Computer_Test.No_through_signal_reverse]
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

    /**
     * Check relays propagate redstone to surrounding blocks.
     *
     * @see [Computer_Test.Set_and_destroy]
     */
    @GameTest
    fun Set_and_destroy(context: GameTestHelper) = context.sequence {
        val lamp = BlockPos(2, 2, 3)

        thenExecute {
            val peripheral = context.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.REDSTONE_RELAY.get())
                .peripheral()
                as RedstoneRelayPeripheral
            peripheral.setOutput(ComputerSide.BACK, true)
        }
        thenIdle(1)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, true, "Lamp should be lit") }
        thenExecute { context.setBlock(BlockPos(2, 2, 2), Blocks.AIR) }
        thenIdle(4)
        thenExecute { context.assertBlockHas(lamp, RedstoneLampBlock.LIT, false, "Lamp should not be lit") }
    }

    /**
     * Check relays pick up propagated redstone to surrounding blocks.
     *
     * @see [Computer_Test.Self_output_update]
     */
    @GameTest
    fun Self_output_update(context: GameTestHelper) = context.sequence {
        fun relay() = context.getBlockEntity(BlockPos(2, 2, 2), ModRegistry.BlockEntities.REDSTONE_RELAY.get())
            .peripheral() as RedstoneRelayPeripheral

        thenExecute { relay().setOutput(ComputerSide.BACK, true) }
        thenIdle(2)
        thenExecute { assertEquals(true, relay().getInput(ComputerSide.BACK), "Input should be on") }

        thenIdle(2)

        thenExecute { relay().setOutput(ComputerSide.BACK, false) }
        thenIdle(2)
        thenExecute { assertEquals(false, relay().getInput(ComputerSide.BACK), "Input should be off") }
    }
}
