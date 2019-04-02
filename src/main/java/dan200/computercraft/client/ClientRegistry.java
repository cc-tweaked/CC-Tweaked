/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.items.ItemTurtleBase;
import dan200.computercraft.shared.util.Colour;
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
import net.minecraftforge.client.event.ColorHandlerEvent;
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
public final class ClientRegistry
{
    private static final String[] EXTRA_MODELS = new String[] {
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

        "turtle_white",
        "turtle_elf_overlay",
    };

    private ClientRegistry() {}

    @SubscribeEvent
    public static void registerModels( ModelRegistryEvent event )
    {
        ModelLoaderRegistry.registerLoader( TurtleModelLoader.INSTANCE );

        // Register item models
        registerUniversalItemModel( ComputerCraft.Items.computer, "computer" );
        registerItemModel( ComputerCraft.Items.commandComputer, 0, "command_computer" );

        registerItemModel( ComputerCraft.Items.pocketComputer, 0, "pocket_computer" );
        registerItemModel( ComputerCraft.Items.pocketComputer, 1, "advanced_pocket_computer" );

        registerItemModel( ComputerCraft.Items.peripheral, 0, "peripheral" );
        registerItemModel( ComputerCraft.Items.peripheral, 1, "wireless_modem" );
        registerItemModel( ComputerCraft.Items.peripheral, 2, "monitor" );
        registerItemModel( ComputerCraft.Items.peripheral, 3, "printer" );
        registerItemModel( ComputerCraft.Items.peripheral, 4, "advanced_monitor" );
        registerItemModel( ComputerCraft.Items.cable, 0, "cable" );
        registerItemModel( ComputerCraft.Items.cable, 1, "wired_modem" );
        registerItemModel( ComputerCraft.Items.advancedModem, 0, "advanced_modem" );
        registerItemModel( ComputerCraft.Items.peripheral, 5, "speaker" );
        registerItemModel( ComputerCraft.Items.wiredModemFull, 0, "wired_modem_full" );

        registerUniversalItemModel( ComputerCraft.Items.disk, "disk" );
        registerItemModel( ComputerCraft.Items.diskExpanded, 0, "disk_expanded" );
        registerItemModel( ComputerCraft.Items.treasureDisk, 0, "treasure_disk" );

        registerItemModel( ComputerCraft.Items.printout, 0, "printout" );
        registerItemModel( ComputerCraft.Items.printout, 1, "pages" );
        registerItemModel( ComputerCraft.Items.printout, 2, "book" );

        registerUniversalItemModel( ComputerCraft.Items.turtle, "turtle" );
        registerUniversalItemModel( ComputerCraft.Items.turtleExpanded, "turtle" );
        registerUniversalItemModel( ComputerCraft.Items.turtleAdvanced, "turtle_advanced" );
    }

    @SubscribeEvent
    public static void onTextureStitchEvent( TextureStitchEvent.Pre event )
    {
        // Load all textures for the extra models
        TextureMap map = event.getMap();
        for( String upgrade : EXTRA_MODELS )
        {
            IModel model = ModelLoaderRegistry.getModelOrMissing( new ResourceLocation( "computercraft", "block/" + upgrade ) );
            for( ResourceLocation texture : model.getTextures() ) map.registerSprite( texture );
        }
    }

    @SubscribeEvent
    public static void onModelBakeEvent( ModelBakeEvent event )
    {
        // Load all extra models
        for( String model : EXTRA_MODELS ) loadBlockModel( event, model );
    }

    @SubscribeEvent
    public static void onItemColours( ColorHandlerEvent.Item event )
    {
        event.getItemColors().registerItemColorHandler(
            ( stack, layer ) -> layer == 1 ? ((ItemDiskLegacy) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            ComputerCraft.Items.disk, ComputerCraft.Items.diskExpanded
        );

        event.getItemColors().registerItemColorHandler( ( stack, layer ) -> {
            switch( layer )
            {
                case 0:
                default:
                    return 0xFFFFFF;
                case 1: // Frame colour
                    return ComputerCraft.Items.pocketComputer.getColour( stack );
                case 2: // Light colour
                {
                    int light = ItemPocketComputer.getLightState( stack );
                    return light == -1 ? Colour.Black.getHex() : light;
                }
            }
        }, ComputerCraft.Items.pocketComputer );

        // Setup turtle colours
        event.getItemColors().registerItemColorHandler(
            ( stack, tintIndex ) -> tintIndex == 0 ? ((ItemTurtleBase) stack.getItem()).getColour( stack ) : 0xFFFFFF,
            ComputerCraft.Blocks.turtle, ComputerCraft.Blocks.turtleExpanded, ComputerCraft.Blocks.turtleAdvanced
        );
    }

    private static void registerItemModel( Item item, int damage, String name )
    {
        ResourceLocation location = new ResourceLocation( ComputerCraft.MOD_ID, name );
        final ModelResourceLocation res = new ModelResourceLocation( location, "inventory" );
        ModelBakery.registerItemVariants( item, location );
        ModelLoader.setCustomModelResourceLocation( item, damage, res );
    }

    private static void registerUniversalItemModel( Item item, String mainModel )
    {
        ResourceLocation mainLocation = new ResourceLocation( ComputerCraft.MOD_ID, mainModel );
        ModelBakery.registerItemVariants( item, mainLocation );

        final ModelResourceLocation mainModelLocation = new ModelResourceLocation( mainLocation, "inventory" );
        ModelLoader.setCustomMeshDefinition( item, new ItemMeshDefinition()
        {
            @Nonnull
            @Override
            public ModelResourceLocation getModelLocation( @Nonnull ItemStack stack )
            {
                return mainModelLocation;
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
