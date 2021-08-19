package dan200.computercraft.ingame.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.*;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber( modid = TestMod.MOD_ID )
public class TestHooks
{
    private static final Logger log = LogManager.getLogger( TestHooks.class );

    private static TestResultList runningTests = null;
    private static int countdown = 20;

    @SubscribeEvent
    public static void onRegisterCommands( RegisterCommandsEvent event )
    {
        log.info( "Starting server, registering command helpers." );
        TestCommand.register( event.getDispatcher() );
        CCTestCommand.register( event.getDispatcher() );
    }

    @SubscribeEvent
    public static void onServerStarted( FMLServerStartedEvent event )
    {
        MinecraftServer server = event.getServer();
        GameRules rules = server.getGameRules();
        rules.getRule( GameRules.RULE_DAYLIGHT ).set( false, server );
        rules.getRule( GameRules.RULE_WEATHER_CYCLE ).set( false, server );
        rules.getRule( GameRules.RULE_DOMOBSPAWNING ).set( false, server );

        ServerWorld world = event.getServer().getLevel( World.OVERWORLD );
        if( world != null ) world.setDayTime( 6000 );

        log.info( "Cleaning up after last run" );
        CommandSource source = server.createCommandSourceStack();
        TestUtils.clearAllTests( source.getLevel(), getStart( source ), TestCollection.singleton, 200 );

        log.info( "Importing files" );
        CCTestCommand.importFiles( server );
    }

    @SubscribeEvent
    public static void onServerTick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        // Let the world settle a bit before starting tests.
        countdown--;
        if( countdown == 0 && System.getProperty( "cctest.run", "false" ).equals( "true" ) ) startTests();

        TestCollection.singleton.tick();
        MainThread.INSTANCE.tick();

        if( runningTests != null && runningTests.isDone() ) finishTests();
    }

    public static TestResultList runTests()
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        CommandSource source = server.createCommandSourceStack();
        Collection<TestFunctionInfo> tests = TestRegistry.getAllTestFunctions()
            .stream()
            .filter( x -> FMLLoader.getDist().isClient() | !x.batchName.startsWith( "client" ) )
            .collect( Collectors.toList() );

        log.info( "Running {} tests...", tests.size() );

        Collection<TestBatch> batches = TestUtils.groupTestsIntoBatches( tests );
        return new TestResultList( TestUtils.runTestBatches(
            batches, getStart( source ), Rotation.NONE, source.getLevel(), TestCollection.singleton, 8
        ) );
    }

    private static void startTests()
    {
        runningTests = runTests();
    }

    private static void finishTests()
    {
        log.info( "Finished tests - {} were run", runningTests.getTotalCount() );
        if( runningTests.hasFailedRequired() )
        {
            log.error( "{} required tests failed", runningTests.getFailedRequiredCount() );
        }
        if( runningTests.hasFailedOptional() )
        {
            log.warn( "{} optional tests failed", runningTests.getFailedOptionalCount() );
        }

        if( ServerLifecycleHooks.getCurrentServer().isDedicatedServer() )
        {
            log.info( "Stopping server." );

            // We can't exit in the main thread, as Minecraft registers a shutdown hook which results
            // in a deadlock. So we do this weird janky thing!
            Thread thread = new Thread( () -> System.exit( runningTests.hasFailedRequired() ? 1 : 0 ) );
            thread.setDaemon( true );
            thread.start();
        }
        else
        {
            shutdownClient();
        }
    }

    private static BlockPos getStart( CommandSource source )
    {
        BlockPos pos = new BlockPos( source.getPosition() );
        return new BlockPos( pos.getX(), source.getLevel().getHeightmapPos( Heightmap.Type.WORLD_SURFACE, pos ).getY(), pos.getZ() + 3 );
    }

    private static void shutdownClient()
    {
        Minecraft.getInstance().clearLevel();
        Minecraft.getInstance().stop();
        if( runningTests.hasFailedOptional() ) System.exit( 1 );
    }
}
