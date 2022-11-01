/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.GameTestHolder
import dan200.computercraft.gametest.api.sequence
import dan200.computercraft.gametest.api.thenOnComputer
import dan200.computercraft.test.core.assertArrayEquals
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.Items

@GameTestHolder
class Disk_Drive_Test {
    /**
     * Ensure audio disks exist and we can play them.
     *
     * @see [#688](https://github.com/cc-tweaked/CC-Tweaked/issues/688)
     */
    @GameTest
    fun Audio_disk(helper: GameTestHelper) = helper.sequence {
        thenOnComputer {
            callPeripheral("right", "hasAudio")
                .assertArrayEquals(true, message = "Disk has audio")

            callPeripheral("right", "getAudioTitle")
                .assertArrayEquals("C418 - 13", message = "Correct audio title")
        }
    }

    @GameTest
    fun Ejects_disk(helper: GameTestHelper) = helper.sequence {
        val stackAt = BlockPos(2, 2, 2)
        thenOnComputer { callPeripheral("right", "ejectDisk") }
        thenWaitUntil { helper.assertItemEntityPresent(Items.MUSIC_DISC_13, stackAt, 0.0) }
    }

    // TODO: Ejecting disk unmounts and closes files
}
