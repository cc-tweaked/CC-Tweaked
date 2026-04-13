// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.api

import com.mojang.serialization.MapCodec
import net.minecraft.gametest.framework.TestEnvironmentDefinition
import net.minecraft.server.level.ServerLevel

/**
 * This annotation defines a method which runs under Minecraft's gametest sequence.
 *
 * Unlike standard game tests, client game tests are only registered when running under the Minecraft client, and run
 * sequentially rather than in parallel.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GameTest(
    /**
     * The template to use for this test.
     */
    val template: String = "",

    /**
     * The timeout for this test.
     */
    val timeoutTicks: Int = Timeouts.DEFAULT,

    /**
     * The number of ticks to wait before the test starts.
     */
    val setupTicks: Int = 0,

    /**
     * Whether this test is required.
     */
    val required: Boolean = true,

    /**
     * The tag associated with this test, denoting when it should run.
     */
    val tag: String = TestTags.COMMON,
)

/**
 * A function that generates
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GameTestGenerator

/**
 * A test environment for client game tests ([TestTags.Client]), which ensures
 * the test runs in a unique batch.
 */
class ClientTestEnvironment(val time: Int = Times.NOON) : TestEnvironmentDefinition<Unit> {
    override fun setup(level: ServerLevel) {
        level.clockManager().setTotalTicks(level.defaultClock, time.toLong())
    }

    override fun teardown(level: ServerLevel, saveData: Unit) {
        level.clockManager().setTotalTicks(level.defaultClock, Times.NOON.toLong())
    }

    override fun codec(): MapCodec<ClientTestEnvironment> = CODEC

    companion object {
        @JvmField
        val CODEC: MapCodec<ClientTestEnvironment> = MapCodec.unit { ClientTestEnvironment() }
    }
}
