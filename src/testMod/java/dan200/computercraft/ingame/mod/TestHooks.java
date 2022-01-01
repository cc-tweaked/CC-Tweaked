/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import dan200.computercraft.ingame.api.Times;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod.EventBusSubscriber( modid = TestMod.MOD_ID )
public class TestHooks
{
    private static final Logger LOG = LogManager.getLogger( TestHooks.class );

    private static MultipleTestTracker runningTests = null;
    private static boolean shutdown = false;
    private static int countdown = 20;

    @SubscribeEvent
    public static void onRegisterCommands( RegisterCommandsEvent event )
    {
        LOG.info( "Starting server, registering command helpers." );
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

        ServerLevel world = event.getServer().getLevel( Level.OVERWORLD );
        if( world != null ) world.setDayTime( Times.NOON );

        LOG.info( "Cleaning up after last run" );
        CommandSourceStack source = server.createCommandSourceStack();
        GameTestRunner.clearAllTests( source.getLevel(), getStart( source ), GameTestTicker.SINGLETON, 200 );

        LOG.info( "Importing files" );
        CCTestCommand.importFiles( server );
    }

    @SubscribeEvent
    public static void onServerTick( TickEvent.ServerTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        // Let the world settle a bit before starting tests.
        countdown--;
        if( countdown == 0 && System.getProperty( "cctest.run", "false" ).equals( "true" ) ) startTests();

        if( !SharedConstants.IS_RUNNING_IN_IDE ) GameTestTicker.SINGLETON.tick();

        if( runningTests != null && runningTests.isDone() ) finishTests();
    }

    public static MultipleTestTracker runTests()
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        CommandSourceStack source = server.createCommandSourceStack();
        Collection<TestFunction> tests = GameTestRegistry.getAllTestFunctions();

        LOG.info( "Running {} tests...", tests.size() );

        return new MultipleTestTracker( GameTestRunner.runTests(
            tests, getStart( source ), Rotation.NONE, source.getLevel(), GameTestTicker.SINGLETON, 8
        ) );
    }

    public static void writeResults( CommandSourceStack source, MultipleTestTracker result )
    {
        if( !result.isDone() ) return;

        say( source, "Finished tests - " + result.getTotalCount() + " tests were run", ChatFormatting.WHITE );
        if( result.hasFailedRequired() )
        {
            say( source, result.getFailedRequiredCount() + " required tests failed :(", ChatFormatting.RED );
        }
        else
        {
            say( source, "All required tests passed :)", ChatFormatting.GREEN );
        }

        if( result.hasFailedOptional() )
        {
            say( source, result.getFailedOptionalCount() + " optional tests failed", ChatFormatting.GRAY );
        }
    }

    private static void startTests()
    {
        runningTests = runTests();
    }

    private static void finishTests()
    {
        if( shutdown ) return;
        shutdown = true;
        writeResults( ServerLifecycleHooks.getCurrentServer().createCommandSourceStack(), runningTests );

        if( FMLLoader.getDist().isDedicatedServer() )
        {
            shutdownServer();
        }
        else
        {
            shutdownClient();
        }
    }

    private static BlockPos getStart( CommandSourceStack source )
    {
        BlockPos pos = new BlockPos( source.getPosition() );
        return new BlockPos( pos.getX(), source.getLevel().getHeightmapPos( Heightmap.Types.WORLD_SURFACE, pos ).getY(), pos.getZ() + 3 );
    }

    public static void shutdownCommon()
    {
        System.exit( runningTests.hasFailedRequired() ? 1 : 0 );
    }

    private static void shutdownServer()
    {
        // We can't exit normally as Minecraft registers a shutdown hook which results in a deadlock.
        LOG.info( "Stopping server." );
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        new Thread( () -> {
            server.halt( true );
            shutdownCommon();
        }, "Background shutdown" ).start();
    }

    private static void shutdownClient()
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute( () -> {
            LOG.info( "Stopping client." );
            minecraft.level.disconnect();
            minecraft.clearLevel();
            minecraft.stop();
            shutdownCommon();
        } );
    }

    private static void say( CommandSourceStack source, String message, ChatFormatting colour )
    {
        source.sendFailure( new TextComponent( message ).withStyle( colour ) );
    }
}
