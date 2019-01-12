/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;

/**
 * Registers textures and models for items.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Side.CLIENT )
public class ClientRegistry
{
    private static final String[] TURTLE_UPGRADES = {
        "turtle_modem_off_left",
        "turtle_modem_on_left",
        "turtle_modem_off_right",
        "turtle_modem_on_right",
        "turtle_crafting_table_left",
        "turtle_crafting_table_right",
        "advanced_turtle_modem_off_left",
        "advanced_turtle_modem_on_left",
        "advanced_turtle_modem_off_right",
        "advanced_turtle_modem_on_right",
        "turtle_speaker_upgrade_left",
        "turtle_speaker_upgrade_right",
    };

    @SubscribeEvent
    public static void registerModels( ModelRegistryEvent event )
    {
        // Register item models
        registerUniversalItemModel( ComputerCraft.Blocks.computer, "computer" );
        registerItemModel( ComputerCraft.Blocks.commandComputer, 0, "command_computer" );

        registerItemModel( ComputerCraft.Items.pocketComputer, 0, "pocket_computer" );
        registerItemModel( ComputerCraft.Items.pocketComputer, 1, "advanced_pocket_computer" );

        registerItemModel( ComputerCraft.Blocks.peripheral, 0, "peripheral" );
        registerItemModel( ComputerCraft.Blocks.peripheral, 1, "wireless_modem" );
        registerItemModel( ComputerCraft.Blocks.peripheral, 2, "monitor" );
        registerItemModel( ComputerCraft.Blocks.peripheral, 3, "printer" );
        registerItemModel( ComputerCraft.Blocks.peripheral, 4, "advanced_monitor" );
        registerItemModel( ComputerCraft.Blocks.cable, 0, "cable" );
        registerItemModel( ComputerCraft.Blocks.cable, 1, "wired_modem" );
        registerItemModel( ComputerCraft.Blocks.advancedModem, 0, "advanced_modem" );
        registerItemModel( ComputerCraft.Blocks.peripheral, 5, "speaker" );
        registerItemModel( ComputerCraft.Blocks.wiredModemFull, 0, "wired_modem_full" );

        registerUniversalItemModel( ComputerCraft.Items.disk, "disk" );
        registerItemModel( ComputerCraft.Items.diskExpanded, 0, "disk_expanded" );
        registerItemModel( ComputerCraft.Items.treasureDisk, 0, "treasure_disk" );

        registerItemModel( ComputerCraft.Items.printout, 0, "printout" );
        registerItemModel( ComputerCraft.Items.printout, 1, "pages" );
        registerItemModel( ComputerCraft.Items.printout, 2, "book" );

        ItemMeshDefinition turtleMeshDefinition = new ItemMeshDefinition()
        {
            private ModelResourceLocation turtle_dynamic = new ModelResourceLocation( "computercraft:turtle_dynamic", "inventory" );

            @Nonnull
            @Override
            public ModelResourceLocation getModelLocation( @Nonnull ItemStack stack )
            {
                return turtle_dynamic;
            }
        };
        String[] turtleModelNames = new String[] {
            "turtle_dynamic",
            "turtle", "turtle_advanced", "turtle_white",
            "turtle_elf_overlay"
        };

        registerUniversalItemModel( ComputerCraft.Blocks.turtle, turtleMeshDefinition, turtleModelNames );
        registerUniversalItemModel( ComputerCraft.Blocks.turtleExpanded, turtleMeshDefinition, turtleModelNames );
        registerUniversalItemModel( ComputerCraft.Blocks.turtleAdvanced, turtleMeshDefinition, turtleModelNames );
    }

    @SubscribeEvent
    public static void onTextureStitchEvent( TextureStitchEvent.Pre event )
    {
        // Load all textures for upgrades
        TextureMap map = event.getMap();
        for( String upgrade : TURTLE_UPGRADES )
        {
            IModel model = ModelLoaderRegistry.getModelOrMissing( new ResourceLocation( "computercraft", "block/" + upgrade ) );
            for( ResourceLocation texture : model.getTextures() )
            {
                map.registerSprite( texture );
            }
        }
    }

    @SubscribeEvent
    public static void onModelBakeEvent( ModelBakeEvent event )
    {
        // Load all upgrade models
        for( String upgrade : TURTLE_UPGRADES )
        {
            loadBlockModel( event, upgrade );
        }
    }

    private static void registerItemModel( Block block, int damage, String name )
    {
        registerItemModel( Item.getItemFromBlock( block ), damage, name );
    }

    private static void registerItemModel( Item item, int damage, String name )
    {
        ResourceLocation location = new ResourceLocation( ComputerCraft.MOD_ID, name );
        final ModelResourceLocation res = new ModelResourceLocation( location, "inventory" );
        ModelBakery.registerItemVariants( item, location );
        ModelLoader.setCustomModelResourceLocation( item, damage, res );
    }

    private static void registerUniversalItemModel( Block block, ItemMeshDefinition definition, String[] names )
    {
        registerUniversalItemModel( Item.getItemFromBlock( block ), definition, names );
    }

    private static void registerUniversalItemModel( Item item, ItemMeshDefinition definition, String[] names )
    {
        ResourceLocation[] resources = new ResourceLocation[names.length];
        for( int i = 0; i < names.length; i++ )
        {
            resources[i] = new ResourceLocation( ComputerCraft.MOD_ID, names[i] );
        }
        ModelBakery.registerItemVariants( item, resources );
        ModelLoader.setCustomMeshDefinition( item, definition );
    }

    private static void registerUniversalItemModel( Block block, String name )
    {
        registerUniversalItemModel( Item.getItemFromBlock( block ), name );
    }

    private static void registerUniversalItemModel( Item item, String name )
    {
        ResourceLocation location = new ResourceLocation( ComputerCraft.MOD_ID, name );
        final ModelResourceLocation res = new ModelResourceLocation( location, "inventory" );
        ModelBakery.registerItemVariants( item, location );
        ModelLoader.setCustomMeshDefinition( item, new ItemMeshDefinition()
        {
            @Nonnull
            @Override
            public ModelResourceLocation getModelLocation( @Nonnull ItemStack stack )
            {
                return res;
            }
        } );
    }

    private static void loadBlockModel( ModelBakeEvent event, String name )
    {
        IModel model = ModelLoaderRegistry.getModelOrMissing( new ResourceLocation( ComputerCraft.MOD_ID, "block/" + name ) );
        IBakedModel bakedModel = model.bake(
            model.getDefaultState(), DefaultVertexFormats.ITEM,
            location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite( location.toString() )
        );

        event.getModelRegistry().putObject( new ModelResourceLocation( ComputerCraft.MOD_ID + ":" + name, "inventory" ), bakedModel );
    }
}
