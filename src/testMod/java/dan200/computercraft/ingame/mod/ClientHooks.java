/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber( modid = TestMod.MOD_ID, value = Dist.CLIENT )
public final class ClientHooks
{
    private static final Logger LOG = LogManager.getLogger( TestHooks.class );

    private static boolean triggered = false;

    private ClientHooks()
    {
    }

    @SubscribeEvent
    public static void onGuiInit( ScreenEvent.Init event )
    {
        if( triggered || !(event.getScreen() instanceof TitleScreen) ) return;
        triggered = true;

        ClientHooks.openWorld();
    }

    private static void openWorld()
    {
        Minecraft minecraft = Minecraft.getInstance();

        // Clear some options before we get any further.
        minecraft.options.autoJump().set( false );
        minecraft.options.cloudStatus().set( CloudStatus.OFF );
        minecraft.options.particles().set( ParticleStatus.MINIMAL );
        minecraft.options.tutorialStep = TutorialSteps.NONE;
        minecraft.options.renderDistance().set( 6 );
        minecraft.options.gamma().set( 1.0 );

        /*
        if( minecraft.getLevelSource().levelExists( "test" ) )
        {
            LOG.info( "World exists, loading it" );
            Minecraft.getInstance().loadLevel( "test" );
        }
        else
        {
            LOG.info( "World does not exist, creating it for the first time" );

            RegistryAccess registries = RegistryAccess.builtinCopy();

            Registry<DimensionType> dimensions = registries.registryOrThrow( Registry.DIMENSION_TYPE_REGISTRY );
            var biomes = registries.registryOrThrow( Registry.BIOME_REGISTRY );
            var structures = registries.registryOrThrow( Registry.STRUCTURE_SET_REGISTRY );

            FlatLevelGeneratorSettings flatSettings = FlatLevelGeneratorSettings.getDefault( biomes, structures )
                .withLayers(
                    Collections.singletonList( new FlatLayerInfo( 4, Blocks.WHITE_CONCRETE ) ),
                    Optional.empty()
                );
            flatSettings.setBiome( biomes.getHolderOrThrow( Biomes.DESERT ) );

            WorldGenSettings generator = new WorldGenSettings( 0, false, false, withOverworld(
                dimensions,
                DimensionType.defaultDimensions( registries, 0 ),
                new FlatLevelSource( structures, flatSettings )
            ) );

            LevelSettings settings = new LevelSettings(
                "test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT
            );
            Minecraft.getInstance().createLevel( "test", settings, registries, generator );
        }
        */
    }
}
