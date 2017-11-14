/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.blocks.BlockCommandComputer;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.items.ItemCommandComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.media.common.DefaultMediaProvider;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemDiskExpanded;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheralProvider;
import dan200.computercraft.shared.peripheral.common.*;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.BlockAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.TileAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import dan200.computercraft.shared.peripheral.modem.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import java.io.File;

public abstract class ComputerCraftProxyCommon implements IComputerCraftProxy
{
    public ComputerCraftProxyCommon()
    {
    }

    // IComputerCraftProxy implementation

    @Override
    public void preInit()
    {
        MinecraftForge.EVENT_BUS.register( this );

        // Creative tab
        ComputerCraft.mainCreativeTab = new CreativeTabMain( CreativeTabs.getNextID() );

        // Recipe types
        // RecipeSorter.register( "computercraft:impostor", ImpostorRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shapeless" );
        // RecipeSorter.register( "computercraft:impostor_shapeless", ImpostorShapelessRecipe.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless" );
        // RecipeSorter.register( "computercraft:disk", DiskRecipe.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless" );
        // RecipeSorter.register( "computercraft:colour", ColourableRecipe.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless" );
        // RecipeSorter.register( "computercraft:printout", PrintoutRecipe.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless" );
        // RecipeSorter.register( "computercraft:pocket_computer_upgrade", PocketComputerUpgradeRecipe.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless" );
    }

    @Override
    public void init()
    {
        registerTileEntities();
        registerForgeHandlers();
    }

    @Override
    public abstract boolean isClient();

    @Override
    public abstract boolean getGlobalCursorBlink();

    @Override
    public abstract long getRenderFrame();

    @Override
    public void deleteDisplayLists( int list, int range )
    {
    }

    @Override
    public abstract Object getFixedWidthFontRenderer();

    @Override
    public String getRecordInfo( @Nonnull ItemStack recordStack )
    {
        Item item = recordStack.getItem();
        if (item instanceof ItemRecord)
        {
            ItemRecord record = (ItemRecord) item;
            return StringUtil.translateToLocal( record.displayName );
        }
        return null;
    }

    @Override
    public abstract void playRecord( SoundEvent record, String recordInfo, World world, BlockPos pos );

    @Override
    public abstract Object getDiskDriveGUI( InventoryPlayer inventory, TileDiskDrive drive );

    @Override
    public abstract Object getComputerGUI( TileComputer computer );

    @Override
    public abstract Object getPrinterGUI( InventoryPlayer inventory, TilePrinter printer );

    @Override
    public abstract Object getTurtleGUI( InventoryPlayer inventory, TileTurtle turtle );

    @Override
    public abstract Object getPrintoutGUI( EntityPlayer player, EnumHand hand );

    @Override
    public abstract Object getPocketComputerGUI( EntityPlayer player, EnumHand hand );

    @Override
    public abstract File getWorldDir( World world );

    @Override
    public void handlePacket( final ComputerCraftPacket packet, final EntityPlayer player )
    {
        IThreadListener listener = player.getServer();
        if (listener != null)
        {
            if (listener.isCallingFromMinecraftThread())
            {
                processPacket( packet, player );
            } else
            {
                listener.addScheduledTask( () -> processPacket( packet, player ) );
            }
        }
    }

    private void processPacket( ComputerCraftPacket packet, EntityPlayer player )
    {
        switch (packet.m_packetType)
        {
            ///////////////////////////////////
            // Packets from Client to Server //
            ///////////////////////////////////
            case ComputerCraftPacket.TurnOn:
            case ComputerCraftPacket.Shutdown:
            case ComputerCraftPacket.Reboot:
            case ComputerCraftPacket.QueueEvent:
            case ComputerCraftPacket.RequestComputerUpdate:
            case ComputerCraftPacket.SetLabel:
            {
                int instance = packet.m_dataInt[0];
                ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instance );
                if (computer != null)
                {
                    computer.handlePacket( packet, player );
                }
                break;
            }
            case ComputerCraftPacket.RequestTileEntityUpdate:
            {
                int x = packet.m_dataInt[0];
                int y = packet.m_dataInt[1];
                int z = packet.m_dataInt[2];
                BlockPos pos = new BlockPos( x, y, z );
                World world = player.getEntityWorld();
                TileEntity tileEntity = world.getTileEntity( pos );
                if (tileEntity != null && tileEntity instanceof TileGeneric)
                {
                    TileGeneric generic = (TileGeneric) tileEntity;
                    SPacketUpdateTileEntity description = generic.getUpdatePacket();
                    if (description != null)
                    {
                        ((EntityPlayerMP) player).connection.sendPacket( description );
                    }
                }
                break;
            }
        }
    }

    @SubscribeEvent
    public void registerBlocks( RegistryEvent.Register<Block> event )
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        // Computer
        ComputerCraft.Blocks.computer = new BlockComputer();
        registry.register( ComputerCraft.Blocks.computer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ) );

        // Peripheral
        ComputerCraft.Blocks.peripheral = new BlockPeripheral();
        registry.register( ComputerCraft.Blocks.peripheral.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "peripheral" ) ) );

        // Cable
        ComputerCraft.Blocks.cable = new BlockCable();
        registry.register( ComputerCraft.Blocks.cable.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "cable" ) ) );

        // Command Computer
        ComputerCraft.Blocks.commandComputer = new BlockCommandComputer();
        registry.register( ComputerCraft.Blocks.commandComputer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "command_computer" ) ) );

        // Command Computer
        ComputerCraft.Blocks.advancedModem = new BlockAdvancedModem();
        registry.register( ComputerCraft.Blocks.advancedModem.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "advanced_modem" ) ) );
    }

    @SubscribeEvent
    public void registerItems( RegistryEvent.Register<Item> event )
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        // Computer
        registry.register( new ItemComputer( ComputerCraft.Blocks.computer ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ) );

        // Peripheral
        registry.register( new ItemPeripheral( ComputerCraft.Blocks.peripheral ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "peripheral" ) ) );

        // Cable
        registry.register( new ItemCable( ComputerCraft.Blocks.cable ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "cable" ) ) );

        // Command Computer
        registry.register( new ItemCommandComputer( ComputerCraft.Blocks.commandComputer ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "command_computer" ) ) );

        // Command Computer
        registry.register( new ItemAdvancedModem( ComputerCraft.Blocks.advancedModem ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "advanced_modem" ) ) );

        // Items
        // Floppy Disk
        ComputerCraft.Items.disk = new ItemDiskLegacy();
        registry.register( ComputerCraft.Items.disk.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk" ) ) );

        ComputerCraft.Items.diskExpanded = new ItemDiskExpanded();
        registry.register( ComputerCraft.Items.diskExpanded.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "disk_expanded" ) ) );

        // Treasure Disk
        ComputerCraft.Items.treasureDisk = new ItemTreasureDisk();
        registry.register( ComputerCraft.Items.treasureDisk.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "treasure_disk" ) ) );

        // Printout
        ComputerCraft.Items.printout = new ItemPrintout();
        registry.register( ComputerCraft.Items.printout.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "printout" ) ) );

        // Pocket computer
        ComputerCraft.Items.pocketComputer = new ItemPocketComputer();
        registry.register( ComputerCraft.Items.pocketComputer.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "pocket_computer" ) ) );
    }

    @SubscribeEvent
    public void registerRecipes( RegistryEvent.Register<IRecipe> event )
    {
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        // Disk
        registry.register( new DiskRecipe().setRegistryName( new ResourceLocation( "computercraft:disk" ) ) );

        // Colourable items (turtles, disks)
        registry.register( new ColourableRecipe().setRegistryName( new ResourceLocation( "computercraft:colour" ) ) );

        // Impostor Disk recipes (to fool NEI)
        ItemStack paper = new ItemStack( Items.PAPER, 1 );
        ItemStack redstone = new ItemStack( Items.REDSTONE, 1 );
        for( int colour = 0; colour < 16; ++colour )
        {
            ItemStack disk = ItemDiskLegacy.createFromIDAndColour( -1, null, Colour.values()[ colour ].getHex() );
            ItemStack dye = new ItemStack( Items.DYE, 1, colour );

            int diskIdx = 0;
            ItemStack[] disks = new ItemStack[ 15 ];
            for( int otherColour = 0; otherColour < 16; ++otherColour )
            {
                if( colour != otherColour )
                {
                    disks[ diskIdx++ ] = ItemDiskLegacy.createFromIDAndColour( -1, null, Colour.values()[ otherColour ].getHex() );
                }
            }

            // Normal recipe
            registry.register(
                new ImpostorShapelessRecipe( "computercraft:disk", disk, new ItemStack[] { redstone, paper, dye } )
                    .setRegistryName( new ResourceLocation( "computercraft:disk_imposter_" + colour ) )
            );

            // Conversion recipe
            registry.register(
                new ImpostorShapelessRecipe( "computercraft:disk", disk, NonNullList.from( Ingredient.EMPTY, Ingredient.fromStacks( disks ), Ingredient.fromStacks( dye ) ) )
                    .setRegistryName( new ResourceLocation( "computercraft:disk_imposter_convert_" + colour ) )
            );
        }

        // Printout
        registry.register( new PrintoutRecipe().setRegistryName( new ResourceLocation( "computercraft:printout" ) ) );

        // Register pocket upgrades
        ComputerCraft.PocketUpgrades.wirelessModem = new PocketModem( false );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModem );
        ComputerCraft.PocketUpgrades.advancedModem = new PocketModem( true );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.advancedModem );

        ComputerCraft.PocketUpgrades.pocketSpeaker = new PocketSpeaker();
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.pocketSpeaker );

        // Wireless Pocket Computer
        registry.register( new PocketComputerUpgradeRecipe().setRegistryName( new ResourceLocation( "computercraft:pocket_computer_upgrade" ) ) );

        // Impostor Pocket Computer recipes (to fool NEI)
        ItemStack pocketComputer = PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, null );
        ItemStack advancedPocketComputer = PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Advanced, null );
        for( IPocketUpgrade upgrade : ComputerCraft.getVanillaPocketUpgrades() )
        {
            registry.register( new ImpostorRecipe(
                    "computercraft:normal_pocket_upgrade",
                    1, 2,
                    new ItemStack[] { upgrade.getCraftingItem(), pocketComputer },
                    PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, upgrade )
                ).setRegistryName( new ResourceLocation( "computercraft:normal_pocket_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) ) )
            );

            registry.register(
                new ImpostorRecipe( "computercraft:advanced_pocket_upgrade",
                    1, 2,
                    new ItemStack[] { upgrade.getCraftingItem(), advancedPocketComputer },
                    PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Advanced, upgrade )
                ).setRegistryName( new ResourceLocation( "computercraft:advanced_pocket_upgrade_" + upgrade.getUpgradeID().toString().replace( ':', '_' ) ) )
            );
        }
    }

    @SubscribeEvent
    public void remapItems( RegistryEvent.MissingMappings<Item> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Item> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getResourceDomain();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getResourcePath();
            if( key.equalsIgnoreCase( "CC-Computer" ) )
            {
                mapping.remap( Item.getItemFromBlock( ComputerCraft.Blocks.computer ) );
            }
            else if( key.equalsIgnoreCase( "CC-Peripheral" ) )
            {
                mapping.remap( Item.getItemFromBlock( ComputerCraft.Blocks.peripheral ) );
            }
            else if( key.equalsIgnoreCase( "CC-Cable" ) )
            {
                mapping.remap( Item.getItemFromBlock( ComputerCraft.Blocks.cable ) );
            }
            else if( key.equalsIgnoreCase( "diskExpanded" ) )
            {
                mapping.remap( ComputerCraft.Items.diskExpanded );
            }
            else if( key.equalsIgnoreCase( "treasureDisk" ) )
            {
                mapping.remap( ComputerCraft.Items.treasureDisk );
            }
            else if( key.equalsIgnoreCase( "pocketComputer" ) )
            {
                mapping.remap( ComputerCraft.Items.pocketComputer );
            }
        }
    }

    @SubscribeEvent
    public void remapBlocks( RegistryEvent.MissingMappings<Block> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Block> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getResourceDomain();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getResourcePath();
            if( key.equalsIgnoreCase( "CC-Computer" ) )
            {
                mapping.remap( ComputerCraft.Blocks.computer );
            }
            else if( key.equalsIgnoreCase( "CC-Peripheral" ) )
            {
                mapping.remap( ComputerCraft.Blocks.peripheral );
            }
            else if( key.equalsIgnoreCase( "CC-Cable" ) )
            {
                mapping.remap( ComputerCraft.Blocks.cable );
            }
        }
    }

    private void registerTileEntities()
    {
        // Tile Entities
        GameRegistry.registerTileEntity( TileComputer.class, ComputerCraft.LOWER_ID + " : " + "computer" );
        GameRegistry.registerTileEntity( TileDiskDrive.class, ComputerCraft.LOWER_ID + " : " + "diskdrive" );
        GameRegistry.registerTileEntity( TileWirelessModem.class, ComputerCraft.LOWER_ID + " : " + "wirelessmodem" );
        GameRegistry.registerTileEntity( TileMonitor.class, ComputerCraft.LOWER_ID + " : " + "monitor" );
        GameRegistry.registerTileEntity( TilePrinter.class, ComputerCraft.LOWER_ID + " : " + "ccprinter" );
        GameRegistry.registerTileEntity( TileCable.class, ComputerCraft.LOWER_ID + " : " + "wiredmodem" );
        GameRegistry.registerTileEntity( TileCommandComputer.class, ComputerCraft.LOWER_ID + " : " + "command_computer" );
        GameRegistry.registerTileEntity( TileAdvancedModem.class, ComputerCraft.LOWER_ID + " : " + "advanced_modem" );
        GameRegistry.registerTileEntity( TileSpeaker.class, ComputerCraft.LOWER_ID + " : " + "speaker" );

        // Register peripheral providers
        ComputerCraftAPI.registerPeripheralProvider( new DefaultPeripheralProvider() );
        if (ComputerCraft.enableCommandBlock)
        {
            ComputerCraftAPI.registerPeripheralProvider( new CommandBlockPeripheralProvider() );
        }

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( new DefaultMediaProvider() );
    }

    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
        NetworkRegistry.INSTANCE.registerGuiHandler( ComputerCraft.instance, handlers );
    }

    public class ForgeHandlers implements
            IGuiHandler
    {
        private ForgeHandlers()
        {
        }

        // IGuiHandler implementation

        @Override
        public Object getServerGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            BlockPos pos = new BlockPos( x, y, z );
            switch (id)
            {
                case ComputerCraft.diskDriveGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TileDiskDrive)
                    {
                        TileDiskDrive drive = (TileDiskDrive) tile;
                        return new ContainerDiskDrive( player.inventory, drive );
                    }
                    break;
                }
                case ComputerCraft.computerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TileComputer)
                    {
                        TileComputer computer = (TileComputer) tile;
                        return new ContainerComputer( computer );
                    }
                    break;
                }
                case ComputerCraft.printerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TilePrinter)
                    {
                        TilePrinter printer = (TilePrinter) tile;
                        return new ContainerPrinter( player.inventory, printer );
                    }
                    break;
                }
                case ComputerCraft.turtleGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TileTurtle)
                    {
                        TileTurtle turtle = (TileTurtle) tile;
                        return new ContainerTurtle( player.inventory, turtle.getAccess(), turtle.getServerComputer() );
                    }
                    break;
                }
                case ComputerCraft.printoutGUIID:
                {
                    return new ContainerHeldItem( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.MAIN_HAND );
                }
                case ComputerCraft.pocketComputerGUIID:
                {
                    return new ContainerPocketComputer( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                }
            }
            return null;
        }

        @Override
        public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            BlockPos pos = new BlockPos( x, y, z );
            switch (id)
            {
                case ComputerCraft.diskDriveGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TileDiskDrive)
                    {
                        TileDiskDrive drive = (TileDiskDrive) tile;
                        return getDiskDriveGUI( player.inventory, drive );
                    }
                    break;
                }
                case ComputerCraft.computerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TileComputer)
                    {
                        TileComputer computer = (TileComputer) tile;
                        return getComputerGUI( computer );
                    }
                    break;
                }
                case ComputerCraft.printerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TilePrinter)
                    {
                        TilePrinter printer = (TilePrinter) tile;
                        return getPrinterGUI( player.inventory, printer );
                    }
                    break;
                }
                case ComputerCraft.turtleGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if (tile != null && tile instanceof TileTurtle)
                    {
                        TileTurtle turtle = (TileTurtle) tile;
                        return getTurtleGUI( player.inventory, turtle );
                    }
                    break;
                }
                case ComputerCraft.printoutGUIID:
                {
                    return getPrintoutGUI( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                }
                case ComputerCraft.pocketComputerGUIID:
                {
                    return getPocketComputerGUI( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                }
            }
            return null;
        }

        // Event handlers

        @SubscribeEvent
        public void onConnectionOpened( FMLNetworkEvent.ClientConnectedToServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public void onConnectionClosed( FMLNetworkEvent.ClientDisconnectionFromServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public void onClientTick( TickEvent.ClientTickEvent event )
        {
            if (event.phase == TickEvent.Phase.START)
            {
                ComputerCraft.clientComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public void onServerTick( TickEvent.ServerTickEvent event )
        {
            if (event.phase == TickEvent.Phase.START)
            {
                MainThread.executePendingTasks();
                ComputerCraft.serverComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public void onWorldLoad( WorldEvent.Load event )
        {
        }

        @SubscribeEvent
        public void onWorldUnload( WorldEvent.Unload event )
        {
        }

        @SubscribeEvent
        public void onConfigChanged( ConfigChangedEvent.OnConfigChangedEvent event) {
            if( event.getModID().equals( ComputerCraft.MOD_ID ) )
            {
                ComputerCraft.syncConfig();
            }
        }

        @SubscribeEvent
        public void onContainerOpen( PlayerContainerEvent.Open event )
        {
            // If we're opening a computer container then broadcast the terminal state
            Container container = event.getContainer();
            if( container instanceof IContainerComputer )
            {
                IComputer computer = ((IContainerComputer) container).getComputer();
                if( computer instanceof ServerComputer )
                {
                    ((ServerComputer) computer).sendTerminalState( event.getEntityPlayer() );
                }
            }
        }
    }
}
