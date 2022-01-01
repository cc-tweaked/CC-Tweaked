/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.settings.CloudOption;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Optional;

import static net.minecraft.world.gen.settings.DimensionGeneratorSettings.withOverworld;

@Mod.EventBusSubscriber( modid = TestMod.MOD_ID, value = Dist.CLIENT )
public final class ClientHooks
{
    private static final Logger LOG = LogManager.getLogger( TestHooks.class );

    private static boolean triggered = false;

    private ClientHooks()
    {
    }

    @SubscribeEvent
    public static void onGuiInit( GuiScreenEvent.InitGuiEvent event )
    {
        if( triggered || !(event.getGui() instanceof MainMenuScreen) ) return;
        triggered = true;

        ClientHooks.openWorld();
    }

    private static void openWorld()
    {
        Minecraft minecraft = Minecraft.getInstance();

        // Clear some options before we get any further.
        minecraft.options.autoJump = false;
        minecraft.options.renderClouds = CloudOption.OFF;
        minecraft.options.particles = ParticleStatus.MINIMAL;
        minecraft.options.tutorialStep = TutorialSteps.NONE;
        minecraft.options.renderDistance = 6;
        minecraft.options.gamma = 1.0;

        if( minecraft.getLevelSource().levelExists( "test" ) )
        {
            LOG.info( "World exists, loading it" );
            Minecraft.getInstance().loadLevel( "test" );
        }
        else
        {
            LOG.info( "World does not exist, creating it for the first time" );

            DynamicRegistries.Impl registries = DynamicRegistries.builtin();

            Registry<DimensionType> dimensions = registries.registryOrThrow( Registry.DIMENSION_TYPE_REGISTRY );
            Registry<Biome> biomes = registries.registryOrThrow( Registry.BIOME_REGISTRY );
            DimensionGeneratorSettings generator = new DimensionGeneratorSettings( 0, false, false, withOverworld(
                dimensions,
                DimensionType.defaultDimensions( dimensions, biomes, registries.registryOrThrow( Registry.NOISE_GENERATOR_SETTINGS_REGISTRY ), 0 ),
                new FlatChunkGenerator( new FlatGenerationSettings(
                    biomes,
                    new DimensionStructuresSettings( Optional.empty(), Collections.emptyMap() ),
                    Collections.singletonList( new FlatLayerInfo( 4, Blocks.WHITE_CONCRETE ) ),
                    false, false,
                    Optional.of( () -> biomes.getOrThrow( Biomes.DESERT ) )
                ) )
            ) );

            WorldSettings settings = new WorldSettings(
                "test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DatapackCodec.DEFAULT
            );
            Minecraft.getInstance().createLevel( "test", settings, registries, generator );
        }
    }
}
