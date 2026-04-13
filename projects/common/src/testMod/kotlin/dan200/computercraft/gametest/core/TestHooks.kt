// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.core.ComputerContext
import dan200.computercraft.core.computer.computerthread.ComputerThread
import dan200.computercraft.gametest.*
import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.computer.core.ServerContext
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.*
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager
import net.minecraft.world.phys.Vec3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import javax.xml.parsers.ParserConfigurationException
import kotlin.jvm.optionals.getOrNull

object TestHooks {
    @JvmField
    val LOG: Logger = LoggerFactory.getLogger(TestHooks::class.java)

    const val MOD_ID: String = "cctest"

    @JvmStatic
    val sourceDir: Path = Paths.get(System.getProperty("cctest.sources")).normalize().toAbsolutePath()

    @JvmStatic
    var structureManager: StructureTemplateManager? = null

    @JvmStatic
    fun init() {
        ServerContext.luaMachine = ManagedComputers
        ComputerCraftAPI.registerAPIFactory(::TestAPI)

        StructureUtils.testStructuresSourceDir = sourceDir
        StructureUtils.testStructuresTargetDir = sourceDir

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

    fun getTestOrigin(server: MinecraftServer): BlockPos {
        val spawn = server.respawnData.pos()
        return BlockPos(spawn.x, -59, spawn.y)
    }

    @JvmStatic
    fun onServerStarted(server: MinecraftServer) {
        val rules = server.gameRules
        rules.set(GameRules.ADVANCE_TIME, false, server)
        server.overworld().clockManager().setTotalTicks(server.overworld().defaultClock, Times.NOON.toLong())

        LOG.info("Cleaning up after last run")

        val level = server.overworld()
        StructureUtils.findTestBlocks(getTestOrigin(server), 200, level).toList().forEach { pos ->
            val test = level.getBlockEntity(pos, BlockEntityType.TEST_INSTANCE_BLOCK).getOrNull() ?: return@forEach
            StructureUtils.clearSpaceForStructure(test.structureBoundingBox, level)
        }

        structureManager = server.structureManager

        ManagedComputers.reset()

        // Delete server context and add one with a mutable machine factory. This allows us to set the factory for
        // specific test batches without having to reset all computers.
        for (computer in ServerContext.get(server).registry().computers) {
            val label = if (computer.label == null) "#" + computer.id else computer.label!!
            LOG.warn("Unexpected computer {}", label)
        }

        LOG.info("Importing files")
        CCTestCommand.importFiles(server)
    }

    @JvmStatic
    fun areComputersIdle(server: MinecraftServer) = ComputerThreadReflection.isFullyIdle(ServerContext.get(server))

    private val testClasses = listOf(
        Component_Test::class.java,
        Computer_Test::class.java,
        CraftOs_Test::class.java,
        Details_Test::class.java,
        Disk_Test::class.java,
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

    private val defaultEnvironment: Holder<TestEnvironmentDefinition<*>> =
        Holder.direct(TestEnvironmentDefinition.AllOf())

    /**
     * Gather a list of all game tests.
     */
    @JvmStatic
    fun loadTests(): List<TestInstance> {
        val tests = mutableListOf<TestInstance>()
        for (testClass in testClasses) {
            for (method in testClass.declaredMethods) {
                registerTest(testClass, method, tests)
            }
        }
        return tests
    }

    private fun registerTest(testClass: Class<*>, method: Method, out: MutableList<TestInstance>) {
        val className = testClass.simpleName.lowercase()
        val testName = className + "." + method.name.lowercase()

        method.getAnnotation(GameTest::class.java)?.let { testInfo ->
            if (!TestTags.isEnabled(testInfo.tag)) return

            val environment: Holder<TestEnvironmentDefinition<*>> = when (testInfo.tag) {
                TestTags.COMMON -> defaultEnvironment
                else -> Holder.direct(ClientTestEnvironment())
            }

            out.add(
                TestInstance(
                    testName,
                    TestData(
                        environment,
                        Identifier.fromNamespaceAndPath(MOD_ID, testInfo.template.ifEmpty { testName }),
                        testInfo.timeoutTicks,
                        testInfo.setupTicks,
                        testInfo.required,
                    ),
                ) { value -> safeInvoke(method, value) },
            )
            return
        }

        if (method.getAnnotation(GameTestGenerator::class.java) != null) {
            val instance =
                if (Modifier.isStatic(method.modifiers)) null else method.declaringClass.getConstructor().newInstance()

            @Suppress("UNCHECKED_CAST")
            val tests = method.invoke(instance) as Collection<TestInstance>
            out.addAll(tests)
        }
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

/**
 * Nasty reflection to determine if computers are fully idle.
 *
 * This is horribly nasty, and should not be used as a model for any production code!
 *
 * @see [ComputerThread.isFullyIdle]
 * @see [dan200.computercraft.mixin.gametest.GameTestServerMixin]
 */
private object ComputerThreadReflection {
    private val lookup = MethodHandles.lookup()

    @JvmField
    val computerContext: MethodHandle = lookup.unreflectGetter(
        ServerContext::class.java.getDeclaredField("context").also { it.isAccessible = true },
    )

    @JvmField
    val isFullyIdle: MethodHandle = lookup.unreflect(
        ComputerThread::class.java.getDeclaredMethod("isFullyIdle").also { it.isAccessible = true },
    )

    fun isFullyIdle(context: ServerContext): Boolean {
        val computerContext = computerContext.invokeExact(context) as ComputerContext
        val computerThread = computerContext.computerScheduler() as ComputerThread
        return isFullyIdle.invokeExact(computerThread) as Boolean
    }
}

class TestInstance(
    val name: String,
    val data: TestData<Holder<TestEnvironmentDefinition<*>>>,
    val function: Consumer<GameTestHelper>,
) {
    val id: Identifier = Identifier.fromNamespaceAndPath(TestHooks.MOD_ID, name)

    val instance: GameTestInstance
        get() =
            FunctionGameTestInstance(ResourceKey.create<Consumer<GameTestHelper>>(Registries.TEST_FUNCTION, id), data)
}
