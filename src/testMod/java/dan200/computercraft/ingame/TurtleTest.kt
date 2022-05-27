package dan200.computercraft.ingame

import dan200.computercraft.ComputerCraft
import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.detail.BasicItemDetailProvider
import dan200.computercraft.ingame.api.Timeouts.COMPUTER_TIMEOUT
import dan200.computercraft.ingame.api.sequence
import dan200.computercraft.ingame.api.thenComputerOk
import dan200.computercraft.shared.media.items.ItemPrintout
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraftforge.gametest.GameTestHolder

@GameTestHolder(ComputerCraft.MOD_ID)
class Turtle_Test {
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Unequip_refreshes_peripheral(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can sheer sheep (and drop items)
     *
     * @see [#537](https://github.com/cc-tweaked/CC-Tweaked/issues/537)
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Shears_sheep(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can place lava.
     *
     * @see [#518](https://github.com/cc-tweaked/CC-Tweaked/issues/518)
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Place_lava(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can place when waterlogged.
     *
     * @see [#385](https://github.com/cc-tweaked/CC-Tweaked/issues/385)
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Place_waterlogged(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can pick up lava
     *
     * @see [#297](https://github.com/cc-tweaked/CC-Tweaked/issues/297)
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Gather_lava(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can hoe dirt.
     *
     * @see [#258](https://github.com/cc-tweaked/CC-Tweaked/issues/258)
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Hoe_dirt(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can place monitors
     *
     * @see [#691](https://github.com/cc-tweaked/CC-Tweaked/issues/691)
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Place_monitor(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can place into compostors. These are non-typical inventories, so
     * worth testing.
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Use_compostors(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can be cleaned in cauldrons.
     *
     * Currently not required as turtles can no longer right-click cauldrons.
     */
    @GameTest(required = false)
    fun Cleaned_with_cauldrons(helper: GameTestHelper) = helper.sequence { thenComputerOk() }

    /**
     * Checks turtles can use IDetailProviders by getting details for a printed page.
     */
    @GameTest(timeoutTicks = COMPUTER_TIMEOUT)
    fun Item_detail_provider(helper: GameTestHelper) = helper.sequence {
        this
            .thenComputerOk(marker = "initial")
            .thenExecute {
                // Register a dummy provider for printout items
                ComputerCraftAPI.registerDetailProvider(
                    ItemStack::class.java,
                    object : BasicItemDetailProvider<ItemPrintout>("printout", ItemPrintout::class.java) {
                        override fun provideDetails(data: MutableMap<in String, Any>, stack: ItemStack, item: ItemPrintout) {
                            data["type"] = item.type.toString();
                        }
                    }
                )
            }
            .thenComputerOk()
    }
}
