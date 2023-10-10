// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.api

import net.minecraft.gametest.framework.GameTest

/**
 * Similar to [GameTest], this annotation defines a method which runs under Minecraft's gametest sequence.
 *
 * Unlike standard game tests, client game tests are only registered when running under the Minecraft client, and run
 * sequentially rather than in parallel.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClientGameTest(
    /**
     * The template to use for this test, identical to [GameTest.template]
     */
    val template: String = "",

    /**
     * The timeout for this test, identical to [GameTest.timeoutTicks].
     */
    val timeoutTicks: Int = Timeouts.DEFAULT,

    /**
     * The tag associated with this test, denoting when it should run.
     */
    val tag: String = TestTags.CLIENT,
)
