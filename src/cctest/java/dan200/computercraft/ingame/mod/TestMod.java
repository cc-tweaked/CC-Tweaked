/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Mod( TestMod.MOD_ID )
public class TestMod
{
    public static final Path sourceDir = Paths.get( "../../src/cctest/server-files/" ).toAbsolutePath();

    public static final String MOD_ID = "cctest";

    public static final Logger log = LogManager.getLogger( MOD_ID );

    private MultipleTestTracker runningTests = null;
    private int countdown = 60;

    public TestMod()
    {
        log.info( "CC: Test initialised" );
        ComputerCraftAPI.registerAPIFactory( TestAPI::new );
        TestLoader.setup();

        StructureUtils.testStructuresDir = sourceDir.resolve( "structures" ).toString();

        MinecraftForge.EVENT_BUS.addListener( ( RegisterCommandsEvent event ) -> {
            log.info( "Starting server, registering command helpers." );
            TestCommand.register( event.getDispatcher() );
            CCTestCommand.register( event.getDispatcher() );
        } );

        MinecraftForge.EVENT_BUS.addListener( ( FMLServerStartedEvent event ) -> {
            MinecraftServer server = event.getServer();
            GameRules rules = server.getGameRules();
            rules.getRule( GameRules.RULE_DAYLIGHT ).set( false, server );
            rules.getRule( GameRules.RULE_WEATHER_CYCLE ).set( false, server );
            rules.getRule( GameRules.RULE_DOMOBSPAWNING ).set( false, server );

            log.info( "Cleaning up after last run" );
            CommandSourceStack source = server.createCommandSourceStack();
            GameTestRunner.clearAllTests( source.getLevel(), getStart( source ), GameTestTicker.SINGLETON, 200 );

            log.info( "Importing files" );
            CCTestCommand.importFiles( server );
        } );

        MinecraftForge.EVENT_BUS.addListener( ( TickEvent.ServerTickEvent event ) -> {
            if( event.phase != TickEvent.Phase.START ) return;

            // Let the world settle a bit before starting tests.
            countdown--;
            if( countdown == 0 && System.getProperty( "cctest.run", "false" ).equals( "true" ) ) startTests();

            GameTestTicker.SINGLETON.tick();

            if( runningTests != null && runningTests.isDone() ) finishTests();
        } );
    }

    private void startTests()
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        CommandSourceStack source = server.createCommandSourceStack();
        Collection<TestFunction> tests = GameTestRegistry.getAllTestFunctions();

        log.info( "Running {} tests...", tests.size() );
        runningTests = new MultipleTestTracker( GameTestRunner.runTests(
            tests, getStart( source ), Rotation.NONE, source.getLevel(), GameTestTicker.SINGLETON, 8
        ) );
    }

    private void finishTests()
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
    }

    private BlockPos getStart( CommandSourceStack source )
    {
        BlockPos pos = new BlockPos( source.getPosition() );
        return new BlockPos( pos.getX(), source.getLevel().getHeightmapPos( Heightmap.Types.WORLD_SURFACE, pos ).getY(), pos.getZ() + 3 );
    }
}
