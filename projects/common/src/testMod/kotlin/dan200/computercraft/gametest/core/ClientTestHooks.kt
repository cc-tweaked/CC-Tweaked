// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import dan200.computercraft.gametest.api.Timeouts
import dan200.computercraft.gametest.api.isRenderingStable
import dan200.computercraft.gametest.api.setupForTest
import net.minecraft.client.CloudStatus
import net.minecraft.client.Minecraft
import net.minecraft.client.ParticleStatus
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.tutorial.TutorialSteps
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.*
import net.minecraft.server.MinecraftServer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Client-side hooks for game tests.
 *
 * This mirrors Minecraft's
 */
object ClientTestHooks {
    private val LOG: Logger = LoggerFactory.getLogger(ClientTestHooks::class.java)

    private const val LEVEL_NAME = "test"

    /**
     * Time (in ticks) that we wait after the client joins the world
     */
    private const val STARTUP_DELAY = 5 * Timeouts.SECOND

    /**
     * Whether our client-side game test driver is enabled.
     */
    private val enabled: Boolean = System.getProperty("cctest.client") != null

    private var loadedWorld: Boolean = false

    @JvmStatic
    fun onOpenScreen(screen: Screen): Boolean = when {
        enabled && !loadedWorld && (screen is TitleScreen || screen is AccessibilityOnboardingScreen) -> {
            loadedWorld = true
            openWorld(screen)
            true
        }

        else -> false
    }

    /**
     * Open or create our test world immediately on game launch.
     */
    private fun openWorld(screen: Screen) {
        val minecraft = Minecraft.getInstance()

        // Clear some options before we get any further.
        minecraft.options.autoJump().set(false)
        minecraft.options.cloudStatus().set(CloudStatus.OFF)
        minecraft.options.particles().set(ParticleStatus.MINIMAL)
        minecraft.options.tutorialStep = TutorialSteps.NONE
        minecraft.options.renderDistance().set(6)
        minecraft.options.gamma().set(1.0)
        minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0)
        minecraft.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(0.0)

        if (minecraft.levelSource.levelExists(LEVEL_NAME)) {
            LOG.info("World already exists, opening.")
            minecraft.createWorldOpenFlows().openWorld(LEVEL_NAME) { minecraft.setScreen(screen) }
        } else {
            LOG.info("World does not exist, creating it.")
            val rules = GameRules()
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null)
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null)
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null)

            minecraft.createWorldOpenFlows().createFreshLevel(
                LEVEL_NAME,
                LevelSettings("Test Level", GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT),
                WorldOptions(WorldOptions.randomSeed(), false, false),
                {
                    it.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions()
                },
                screen,
            )
        }
    }

    private var testTracker: MultipleTestTracker? = null
    private var hasFinished: Boolean = false
    private var startupDelay: Int = STARTUP_DELAY

    @JvmStatic
    fun onServerTick(server: MinecraftServer) {
        if (!enabled || hasFinished) return

        val testTracker = when (val tracker = this.testTracker) {
            null -> {
                if (server.overworld().players().isEmpty()) return
                if (!Minecraft.getInstance().isRenderingStable()) return
                if (startupDelay >= 0) {
                    // TODO: Is there a better way? Maybe set a flag when the client starts rendering?
                    startupDelay--
                    return
                }

                LOG.info("Server ready, starting.")

                val tests = GameTestRunner.Builder.fromBatches(
                    GameTestBatchFactory.fromTestFunction(GameTestRegistry.getAllTestFunctions(), server.overworld()),
                    server.overworld(),
                )
                    .newStructureSpawner(StructureGridSpawner(TestHooks.getTestOrigin(server), 8, false))
                    .build()

                val testTracker = MultipleTestTracker(tests.testInfos)
                testTracker.addListener(
                    object : GameTestListener {
                        override fun testPassed(test: GameTestInfo, runner: GameTestRunner) = testFinished()
                        override fun testFailed(test: GameTestInfo, runner: GameTestRunner) = testFinished()
                        override fun testStructureLoaded(test: GameTestInfo) = Unit
                        override fun testAddedForRerun(test: GameTestInfo, newTest: GameTestInfo, runner: GameTestRunner) {
                        }

                        fun testFinished() {
                            for (it in server.playerList.players) it.setupForTest()
                        }
                    },
                )

                tests.start()

                LOG.info("{} tests are now running!", testTracker.totalCount)
                this.testTracker = testTracker
                testTracker
            }

            else -> tracker
        }

        if (server.overworld().gameTime % 20L == 0L) LOG.info(testTracker.progressBar)

        if (testTracker.isDone) {
            hasFinished = true
            LOG.info(testTracker.progressBar)

            GlobalTestReporter.finish()
            LOG.info("========= {} GAME TESTS COMPLETE ======================", testTracker.totalCount)
            if (testTracker.hasFailedRequired()) {
                LOG.info("{} required tests failed :(", testTracker.failedRequiredCount)
                for (test in testTracker.failedRequired) LOG.info("   - {}", test.testName)
            } else {
                LOG.info("All {} required tests passed :)", testTracker.totalCount)
            }
            if (testTracker.hasFailedOptional()) {
                LOG.info("{} optional tests failed", testTracker.failedOptionalCount)
                for (test in testTracker.failedOptional) LOG.info("   - {}", test.testName)
            }
            LOG.info("====================================================")

            // Stop Minecraft *from the client thread*. We need to do this to avoid deadlocks in stopping the server.
            val minecraft = Minecraft.getInstance()
            minecraft.execute {
                LOG.info("Stopping client.")
                minecraft.level!!.disconnect()
                minecraft.disconnect()
                minecraft.stop()

                exitProcess(
                    when {
                        testTracker.totalCount == 0 -> 1
                        testTracker.hasFailedRequired() -> 2
                        else -> 0
                    },
                )
            }
        }
    }
}
