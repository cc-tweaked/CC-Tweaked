package dan200.computercraft.ingame

import dan200.computercraft.ingame.api.*
import net.minecraft.entity.item.ItemEntity
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals

class DiskDriveTest {
    /**
     * Ensure audio disks exist and we can play them.
     *
     * @see [#688](https://github.com/SquidDev-CC/CC-Tweaked/issues/688)
     */
    @GameTest
    suspend fun `Audio disk`(context: TestContext) = context.checkComputerOk(3)

    @GameTest
    suspend fun `Ejects disk`(context: TestContext) {
        val stackAt = BlockPos(2, 0, 2)
        context.checkComputerOk(4)
        context.waitUntil { context.getEntity(stackAt) != null }

        val stack = context.getEntityOfType<ItemEntity>(stackAt)!!
        assertEquals(Items.MUSIC_DISC_13, stack.item.item, "Correct item stack")
    }
}
