package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenComputerOk
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.Items
import net.minecraftforge.gametest.GameTestHolder

@GameTestHolder(ComputerCraft.MOD_ID)
class Disk_Drive_Test {
    /**
     * Ensure audio disks exist and we can play them.
     *
     * @see [#688](https://github.com/cc-tweaked/CC-Tweaked/issues/688)
     */
    @GameTest
    fun Audio_disk(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    @GameTest
    fun Ejects_disk(helper: GameTestHelper) = helper.sequence {
        val stackAt = BlockPos(2, 2, 2)
        this
            .thenComputerOk()
            .thenWaitUntil { helper.assertItemEntityPresent(Items.MUSIC_DISC_13, stackAt, 0.0) }
    }
}
