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
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.ContainerViewComputer;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.computer.blocks.BlockCommandComputer;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileCommandComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.*;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.items.ItemCommandComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.datafix.Fixes;
import dan200.computercraft.shared.integration.charset.IntegrationCharset;
import dan200.computercraft.shared.media.common.DefaultMediaProvider;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemDiskExpanded;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.client.*;
import dan200.computercraft.shared.network.server.ComputerActionServerMessage;
import dan200.computercraft.shared.network.server.ComputerServerMessage;
import dan200.computercraft.shared.network.server.QueueEventServerMessage;
import dan200.computercraft.shared.network.server.RequestComputerMessage;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheralProvider;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.DefaultPeripheralProvider;
import dan200.computercraft.shared.peripheral.common.ItemPeripheral;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.*;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.ItemAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.TileWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.peripheral.speaker.TileSpeaker;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.block.Block;
import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistry;
import pl.asie.charset.ModCharset;

public abstract class ComputerCraftProxyCommon implements IComputerCraftProxy
{
    @Override
    public void preInit()
    {
        MinecraftForge.EVENT_BUS.register( this );

        // Creative tab
        ComputerCraft.mainCreativeTab = new CreativeTabMain( CreativeTabs.getNextID() );
    }

    @Override
    public void init()
    {
        registerTileEntities();
        registerForgeHandlers();
        registerNetwork();

        Fixes.register( FMLCommonHandler.instance().getDataFixer() );
        if( Loader.isModLoaded( ModCharset.MODID ) ) IntegrationCharset.register();
    }

    @Override
    public void initServer( MinecraftServer server )
    {
        CommandHandler handler = (CommandHandler) server.getCommandManager();
        handler.registerCommand( new CommandComputerCraft() );
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

        // Full block modem
        ComputerCraft.Blocks.wiredModemFull = new BlockWiredModemFull();
        registry.register( ComputerCraft.Blocks.wiredModemFull.setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full" ) ) );
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

        // Advanced modem
        registry.register( new ItemAdvancedModem( ComputerCraft.Blocks.advancedModem ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "advanced_modem" ) ) );

        // Full block modem
        registry.register( new ItemWiredModemFull( ComputerCraft.Blocks.wiredModemFull ).setRegistryName( new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full" ) ) );

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

        registerUpgrades();
    }

    @SubscribeEvent
    public void registerRecipes( RegistryEvent.Register<IRecipe> event )
    {
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        // Impostor Disk recipes (to fool NEI)
        ItemStack paper = new ItemStack( Items.PAPER, 1 );
        ItemStack redstone = new ItemStack( Items.REDSTONE, 1 );
        for( int colour = 0; colour < 16; ++colour )
        {
            ItemStack disk = ItemDiskLegacy.createFromIDAndColour( -1, null, Colour.values()[colour].getHex() );
            ItemStack dye = new ItemStack( Items.DYE, 1, colour );

            int diskIdx = 0;
            ItemStack[] disks = new ItemStack[15];
            for( int otherColour = 0; otherColour < 16; ++otherColour )
            {
                if( colour != otherColour )
                {
                    disks[diskIdx++] = ItemDiskLegacy.createFromIDAndColour( -1, null, Colour.values()[otherColour].getHex() );
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

        // Impostor Pocket Computer recipes (to fool NEI)
        ItemStack pocketComputer = PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Normal, null );
        ItemStack advancedPocketComputer = PocketComputerItemFactory.create( -1, null, -1, ComputerFamily.Advanced, null );
        for( IPocketUpgrade upgrade : PocketUpgrades.getVanillaUpgrades() )
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

    private void registerUpgrades()
    {
        // Register pocket upgrades
        ComputerCraft.PocketUpgrades.wirelessModem = new PocketModem( false );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.wirelessModem );
        ComputerCraft.PocketUpgrades.advancedModem = new PocketModem( true );
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.advancedModem );

        ComputerCraft.PocketUpgrades.pocketSpeaker = new PocketSpeaker();
        ComputerCraftAPI.registerPocketUpgrade( ComputerCraft.PocketUpgrades.pocketSpeaker );
    }

    @SubscribeEvent
    public void remapItems( RegistryEvent.MissingMappings<Item> mappings )
    {
        // We have to use mappings.getAllMappings() as the mod ID is upper case but the domain lower.
        for( RegistryEvent.MissingMappings.Mapping<Item> mapping : mappings.getAllMappings() )
        {
            String domain = mapping.key.getNamespace();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getPath();
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
            String domain = mapping.key.getNamespace();
            if( !domain.equalsIgnoreCase( ComputerCraft.MOD_ID ) ) continue;

            String key = mapping.key.getPath();
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
        GameRegistry.registerTileEntity( TileComputer.class, new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) );
        GameRegistry.registerTileEntity( TileDiskDrive.class, new ResourceLocation( ComputerCraft.MOD_ID, "diskdrive" ) );
        GameRegistry.registerTileEntity( TileWirelessModem.class, new ResourceLocation( ComputerCraft.MOD_ID, "wirelessmodem" ) );
        GameRegistry.registerTileEntity( TileMonitor.class, new ResourceLocation( ComputerCraft.MOD_ID, "monitor" ) );
        GameRegistry.registerTileEntity( TilePrinter.class, new ResourceLocation( ComputerCraft.MOD_ID, "ccprinter" ) );
        GameRegistry.registerTileEntity( TileCable.class, new ResourceLocation( ComputerCraft.MOD_ID, "wiredmodem" ) );
        GameRegistry.registerTileEntity( TileCommandComputer.class, new ResourceLocation( ComputerCraft.MOD_ID, "command_computer" ) );
        GameRegistry.registerTileEntity( TileAdvancedModem.class, new ResourceLocation( ComputerCraft.MOD_ID, "advanced_modem" ) );
        GameRegistry.registerTileEntity( TileSpeaker.class, new ResourceLocation( ComputerCraft.MOD_ID, "speaker" ) );
        GameRegistry.registerTileEntity( TileWiredModemFull.class, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full" ) );

        // Register peripheral providers
        ComputerCraftAPI.registerPeripheralProvider( new DefaultPeripheralProvider() );
        if( ComputerCraft.enableCommandBlock )
        {
            ComputerCraftAPI.registerPeripheralProvider( new CommandBlockPeripheralProvider() );
        }

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( new DefaultMediaProvider() );

        // Register network providers
        CapabilityWiredElement.register();
    }

    private void registerForgeHandlers()
    {
        ForgeHandlers handlers = new ForgeHandlers();
        MinecraftForge.EVENT_BUS.register( handlers );
        NetworkRegistry.INSTANCE.registerGuiHandler( ComputerCraft.instance, handlers );
    }

    private void registerNetwork()
    {
        // Server messages

        ComputerServerMessage.register( ComputerActionServerMessage::new, ( computer, packet ) -> {
            switch( packet.getAction() )
            {
                case TURN_ON:
                    computer.turnOn();
                    break;
                case REBOOT:
                    computer.reboot();
                    break;
                case SHUTDOWN:
                    computer.shutdown();
                    break;
            }
        } );

        ComputerServerMessage.register( QueueEventServerMessage::new, ( computer, packet ) ->
            computer.queueEvent( packet.getEvent(), packet.getArgs() ) );

        NetworkMessage.registerMainThread( Side.SERVER, RequestComputerMessage::new, ( context, packet ) -> {
            ServerComputer computer = ComputerCraft.serverComputerRegistry.get( packet.getInstance() );
            if( computer != null ) computer.sendComputerState( context.getServerHandler().player );
        } );

        // Client messages

        NetworkMessage.registerMainThread( Side.CLIENT, PlayRecordClientMessage::new, ( computer, packet ) ->
            playRecordClient( packet.getPos(), packet.getSoundEvent(), packet.getName() ) );

        ComputerClientMessage.register( ComputerDataClientMessage::new, ( computer, packet ) ->
            computer.setState( packet.getState(), packet.getUserData() ) );

        ComputerClientMessage.register( ComputerTerminalClientMessage::new, ( computer, packet ) ->
            computer.readDescription( packet.getTag() ) );

        NetworkMessage.registerMainThread( Side.CLIENT, ComputerDeletedClientMessage::new, ( context, packet ) ->
            ComputerCraft.clientComputerRegistry.remove( packet.getInstanceId() ) );

        NetworkMessage.registerMainThread( Side.CLIENT, ChatTableClientMessage::new, ( context, packet ) ->
            showTableClient( packet.getTable() ) );
    }

    public class ForgeHandlers implements IGuiHandler
    {
        private ForgeHandlers()
        {
        }

        // IGuiHandler implementation

        @Override
        public Object getServerGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            BlockPos pos = new BlockPos( x, y, z );
            switch( id )
            {
                case ComputerCraft.diskDriveGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileDiskDrive )
                    {
                        TileDiskDrive drive = (TileDiskDrive) tile;
                        return new ContainerDiskDrive( player.inventory, drive );
                    }
                    break;
                }
                case ComputerCraft.computerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileComputer )
                    {
                        TileComputer computer = (TileComputer) tile;
                        return new ContainerComputer( computer );
                    }
                    break;
                }
                case ComputerCraft.printerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TilePrinter )
                    {
                        TilePrinter printer = (TilePrinter) tile;
                        return new ContainerPrinter( player.inventory, printer );
                    }
                    break;
                }
                case ComputerCraft.turtleGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileTurtle )
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
                case ComputerCraft.viewComputerGUIID:
                {
                    ServerComputer computer = ComputerCraft.serverComputerRegistry.get( x );
                    return computer == null ? null : new ContainerViewComputer( computer );
                }
            }
            return null;
        }

        @Override
        public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            BlockPos pos = new BlockPos( x, y, z );
            switch( id )
            {
                case ComputerCraft.diskDriveGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileDiskDrive )
                    {
                        TileDiskDrive drive = (TileDiskDrive) tile;
                        return getDiskDriveGUI( player.inventory, drive );
                    }
                    break;
                }
                case ComputerCraft.computerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileComputer )
                    {
                        TileComputer computer = (TileComputer) tile;
                        return getComputerGUI( computer );
                    }
                    break;
                }
                case ComputerCraft.printerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TilePrinter )
                    {
                        TilePrinter printer = (TilePrinter) tile;
                        return getPrinterGUI( player.inventory, printer );
                    }
                    break;
                }
                case ComputerCraft.turtleGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileTurtle )
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
                case ComputerCraft.viewComputerGUIID:
                {
                    ClientComputer computer = ComputerCraft.clientComputerRegistry.get( x );

                    // We extract some terminal information from the various coordinate flags.
                    // See ComputerCraft.openComputerGUI for how they are packed.
                    ComputerFamily family = ComputerFamily.values()[y];
                    int width = (z >> 16) & 0xFFFF, height = z & 0xFF;

                    if( computer == null )
                    {
                        computer = new ClientComputer( x );
                        ComputerCraft.clientComputerRegistry.add( x, computer );
                    }
                    else if( computer.getTerminal() != null )
                    {
                        width = computer.getTerminal().getWidth();
                        height = computer.getTerminal().getHeight();
                    }
                    return getComputerGUI( computer, width, height, family );
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
            if( event.phase == TickEvent.Phase.START )
            {
                ComputerCraft.clientComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public void onServerTick( TickEvent.ServerTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
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
        public void onConfigChanged( ConfigChangedEvent.OnConfigChangedEvent event )
        {
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
