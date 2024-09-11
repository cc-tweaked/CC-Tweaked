// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.*
import net.minecraft.gametest.framework.GameTestGenerator
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestFunction

class Printout_Test {
    /**
     * Test printouts render correctly
     */
    @GameTestGenerator
    fun Render_in_frame(): List<TestFunction> {
        val tests = mutableListOf<TestFunction>()

        fun addTest(label: String, time: Long = Times.NOON, tag: String = TestTags.CLIENT) {
            if (!TestTags.isEnabled(tag)) return

            val className = this::class.java.simpleName.lowercase()
            val testName = "$className.render_in_frame"

            tests.add(
                TestFunction(
                    "$testName.$label",
                    "$testName.$label",
                    testName,
                    Timeouts.DEFAULT,
                    0,
                    true,
                ) { renderPrintout(it, time) },
            )
        }

        addTest("noon", Times.NOON)
        addTest("midnight", Times.MIDNIGHT)

        addTest("sodium", tag = "sodium")

        addTest("iris_noon", Times.NOON, tag = "iris")
        addTest("iris_midnight", Times.MIDNIGHT, tag = "iris")

        return tests
    }

    private fun renderPrintout(helper: GameTestHelper, time: Long) = helper.sequence {
        thenExecute {
            helper.level.dayTime = time
            helper.positionAtArmorStand()
        }

        thenScreenshot()

        thenExecute { helper.level.dayTime = Times.NOON }
    }
}
