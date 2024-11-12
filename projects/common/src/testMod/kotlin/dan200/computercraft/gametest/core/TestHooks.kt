// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.gametest.*
import dan200.computercraft.gametest.api.ClientGameTest
import dan200.computercraft.gametest.api.TestTags
import dan200.computercraft.gametest.api.Times
import dan200.computercraft.shared.computer.core.ServerContext
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import javax.xml.parsers.ParserConfigurationException

object TestHooks {
    @JvmField
    val LOG: Logger = LoggerFactory.getLogger(TestHooks::class.java)

    @JvmStatic
    val sourceDir: Path = Paths.get(System.getProperty("cctest.sources")).normalize().toAbsolutePath()

    @JvmStatic
    fun init() {
        ServerContext.luaMachine = ManagedComputers
        ComputerCraftAPI.registerAPIFactory(::TestAPI)
        StructureUtils.testStructuresDir = sourceDir.resolve("structures").toString()

        // Set up our test reporter if configured.
        val outputPath = System.getProperty("cctest.gametest-report")
        if (outputPath != null) {
            try {
                GlobalTestReporter.replaceWith(
                    MultiTestReporter(
                        JunitTestReporter(File(outputPath)),
                        LogTestReporter(),
                    ),
                )
            } catch (e: ParserConfigurationException) {
                throw RuntimeException(e)
            }
        }
    }

    @JvmStatic
    fun onServerStarted(server: MinecraftServer) {
        val rules = server.gameRules
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server)
        server.overworld().dayTime = Times.NOON

        LOG.info("Cleaning up after last run")
        GameTestRunner.clearAllTests(server.overworld(), BlockPos(0, -60, 0), GameTestTicker.SINGLETON, 200)

        // Delete server context and add one with a mutable machine factory. This allows us to set the factory for
        // specific test batches without having to reset all computers.
        for (computer in ServerContext.get(server).registry().computers) {
            val label = if (computer.label == null) "#" + computer.id else computer.label!!
            LOG.warn("Unexpected computer {}", label)
        }

        LOG.info("Importing files")
        CCTestCommand.importFiles(server)
    }

    private val testClasses = listOf(
        Computer_Test::class.java,
        CraftOs_Test::class.java,
        Disk_Drive_Test::class.java,
        Inventory_Test::class.java,
        Loot_Test::class.java,
        Modem_Test::class.java,
        Monitor_Test::class.java,
        Pocket_Computer_Test::class.java,
        Printer_Test::class.java,
        Printout_Test::class.java,
        Recipe_Test::class.java,
        Relay_Test::class.java,
        Speaker_Test::class.java,
        Turtle_Test::class.java,
    )

    /**
     * Register all of our gametests.
     *
     * This is super nasty, as it bypasses any loader-specific hooks for registering tests. However, it makes it much
     * easier to ensure consistent behaviour between loaders (namely making [GameTest.template] point to a
     * structure rather than a per-test-class one), as well as supporting our custom client tests.
     *
     * @param fallbackRegister A fallback function which registers non-test methods (such as [BeforeBatch]). This
     * should be [GameTestRegistry.register] or equivalent.
     */
    @JvmStatic
    fun loadTests(fallbackRegister: Consumer<Method>) {
        for (testClass in testClasses) {
            for (method in testClass.declaredMethods) {
                registerTest(testClass, method, fallbackRegister)
            }
        }
    }

    private val isCi = System.getenv("CI") != null

    /**
     * Adjust the timeout of a test. This makes it 1.5 times longer when run under CI, as CI servers are less powerful
     * than our own.
     */
    private fun adjustTimeout(timeout: Int): Int = if (isCi) timeout + (timeout / 2) else timeout

    private fun registerTest(testClass: Class<*>, method: Method, fallbackRegister: Consumer<Method>) {
        val className = testClass.simpleName.lowercase()
        val testName = className + "." + method.name.lowercase()

        method.getAnnotation(GameTest::class.java)?.let { testInfo ->
            if (!TestTags.isEnabled(TestTags.COMMON)) return

            GameTestRegistry.getAllTestFunctions().add(
                TestFunction(
                    testInfo.batch, testName, testInfo.template.ifEmpty { testName },
                    StructureUtils.getRotationForRotationSteps(testInfo.rotationSteps),
                    adjustTimeout(testInfo.timeoutTicks),
                    testInfo.setupTicks,
                    testInfo.required, testInfo.requiredSuccesses, testInfo.attempts,
                ) { value -> safeInvoke(method, value) },
            )
            GameTestRegistry.getAllTestClassNames().add(testClass.simpleName)
            return
        }

        method.getAnnotation(ClientGameTest::class.java)?.let { testInfo ->
            if (!TestTags.isEnabled(testInfo.tag)) return

            GameTestRegistry.getAllTestFunctions().add(
                TestFunction(
                    testName,
                    testName,
                    testInfo.template.ifEmpty { testName },
                    adjustTimeout(testInfo.timeoutTicks),
                    0,
                    true,
                ) { value -> safeInvoke(method, value) },
            )
            GameTestRegistry.getAllTestClassNames().add(testClass.simpleName)
            return
        }

        fallbackRegister.accept(method)
    }

    private fun safeInvoke(method: Method, value: Any) {
        try {
            var instance: Any? = null
            if (!Modifier.isStatic(method.modifiers)) {
                instance = method.declaringClass.getConstructor().newInstance()
            }
            method.invoke(instance, value)
        } catch (e: InvocationTargetException) {
            when (val cause = e.cause) {
                is RuntimeException -> throw cause
                else -> throw RuntimeException(cause)
            }
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Adds a hook that makes breaking a bone block spawn an explosion.
     *
     * It would be more Correct to register a custom block, but that's quite a lot of work, and doesn't seem worth it
     * for test code.
     *
     * See also [Turtle_Test.Breaks_exploding_block].
     */
    @JvmStatic
    fun onBeforeDestroyBlock(level: LevelAccessor, pos: BlockPos, state: BlockState): Boolean {
        if (state.block === Blocks.BONE_BLOCK && level is ServerLevel) {
            val explosionPos = Vec3.atCenterOf(pos)
            level.explode(null, explosionPos.x, explosionPos.y, explosionPos.z, 4.0f, Level.ExplosionInteraction.TNT)
            return true
        }

        return false
    }
}
