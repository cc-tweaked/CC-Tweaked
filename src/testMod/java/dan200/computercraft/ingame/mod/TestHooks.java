/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import dan200.computercraft.ingame.api.Times;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber( modid = TestMod.MOD_ID )
public class TestHooks
{
    private static final Logger LOG = LogManager.getLogger( TestHooks.class );

    @SubscribeEvent
    public static void onRegisterCommands( RegisterCommandsEvent event )
    {
        LOG.info( "Starting server, registering command helpers." );
        CCTestCommand.register( event.getDispatcher() );
    }

    @SubscribeEvent
    public static void onServerStarted( ServerStartedEvent event )
    {
        MinecraftServer server = event.getServer();
        GameRules rules = server.getGameRules();
        rules.getRule( GameRules.RULE_DAYLIGHT ).set( false, server );

        ServerLevel world = event.getServer().getLevel( Level.OVERWORLD );
        if( world != null ) world.setDayTime( Times.NOON );

        // LOG.info( "Cleaning up after last run" );
        // CommandSourceStack source = server.createCommandSourceStack();
        // GameTestRunner.clearAllTests( source.getLevel(), getStart( source ), GameTestTicker.SINGLETON, 200 );

        LOG.info( "Importing files" );
        CCTestCommand.importFiles( server );
    }
}
