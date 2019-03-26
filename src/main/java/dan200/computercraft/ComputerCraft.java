/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.permissions.ITurtlePermissionProvider;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.AddressPredicate;
import dan200.computercraft.core.apis.ApiFactories;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.filesystem.ComboMount;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.JarMount;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.*;
import dan200.computercraft.shared.computer.blocks.BlockCommandComputer;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputerRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.computer.items.ItemCommandComputer;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.media.items.ItemDiskExpanded;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.ItemPeripheral;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wired.ItemCable;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.ItemAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.proxy.ICCTurtleProxy;
import dan200.computercraft.shared.proxy.IComputerCraftProxy;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtleAdvanced;
import dan200.computercraft.shared.turtle.items.ItemTurtleLegacy;
import dan200.computercraft.shared.turtle.items.ItemTurtleNormal;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.util.IoUtil;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import dan200.computercraft.shared.wired.WiredNode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(
    modid = ComputerCraft.MOD_ID, name = "CC: Tweaked", version = "${version}",
    guiFactory = "dan200.computercraft.client.gui.GuiConfigCC$Factory",
    dependencies = "required:forge@[14.23.4.2746,)"
)
public class ComputerCraft
{
    public static final String MOD_ID = "computercraft";

    // GUI IDs
    public static final int diskDriveGUIID = 100;
    public static final int computerGUIID = 101;
    public static final int printerGUIID = 102;
    public static final int turtleGUIID = 103;
    // ComputerCraftEdu uses ID 104
    public static final int printoutGUIID = 105;
    public static final int pocketComputerGUIID = 106;
    public static final int viewComputerGUIID = 110;

    // Configuration options
    public static final String[] DEFAULT_HTTP_WHITELIST = new String[] { "*" };
    public static final String[] DEFAULT_HTTP_BLACKLIST = new String[] {
        "127.0.0.0/8",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "fd00::/8",
    };

    public static int computerSpaceLimit = 1000 * 1000;
    public static int floppySpaceLimit = 125 * 1000;
    public static int maximumFilesOpen = 128;
    public static boolean disable_lua51_features = false;
    public static String default_computer_settings = "";
    public static boolean debug_enable = true;
    public static boolean logPeripheralErrors = false;

    public static int computer_threads = 1;
    public static long maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( 10 );
    public static long maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( 5 );

    public static boolean http_enable = true;
    public static boolean http_websocket_enable = true;
    public static AddressPredicate http_whitelist = new AddressPredicate( DEFAULT_HTTP_WHITELIST );
    public static AddressPredicate http_blacklist = new AddressPredicate( DEFAULT_HTTP_BLACKLIST );

    public static int httpTimeout = 30000;
    public static int httpMaxRequests = 16;
    public static long httpMaxDownload = 16 * 1024 * 1024;
    public static long httpMaxUpload = 4 * 1024 * 1024;
    public static int httpMaxWebsockets = 4;
    public static int httpMaxWebsocketMessage = Websocket.MAX_MESSAGE_SIZE;

    public static boolean enableCommandBlock = false;
    public static int modem_range = 64;
    public static int modem_highAltitudeRange = 384;
    public static int modem_rangeDuringStorm = 64;
    public static int modem_highAltitudeRangeDuringStorm = 384;
    public static int maxNotesPerTick = 8;

    public static boolean turtlesNeedFuel = true;
    public static int turtleFuelLimit = 20000;
    public static int advancedTurtleFuelLimit = 100000;
    public static boolean turtlesObeyBlockProtection = true;
    public static boolean turtlesCanPush = true;
    public static EnumSet<TurtleAction> turtleDisabledActions = EnumSet.noneOf( TurtleAction.class );

    public static final int terminalWidth_computer = 51;
    public static final int terminalHeight_computer = 19;

    public static final int terminalWidth_turtle = 39;
    public static final int terminalHeight_turtle = 13;

    public static final int terminalWidth_pocketComputer = 26;
    public static final int terminalHeight_pocketComputer = 20;

    // Blocks and Items
    public static class Blocks
    {
        public static BlockComputer computer;
        public static BlockCommandComputer commandComputer;

        public static BlockTurtle turtle;
        public static BlockTurtle turtleExpanded;
        public static BlockTurtle turtleAdvanced;

        public static BlockPeripheral peripheral;
        public static BlockCable cable;
        public static BlockAdvancedModem advancedModem;
        public static BlockWiredModemFull wiredModemFull;
    }

    public static class Items
    {
        public static ItemComputer computer;
        public static ItemCommandComputer commandComputer;

        public static ItemTurtleLegacy turtle;
        public static ItemTurtleNormal turtleExpanded;
        public static ItemTurtleAdvanced turtleAdvanced;

        public static ItemPocketComputer pocketComputer;

        public static ItemDiskLegacy disk;
        public static ItemDiskExpanded diskExpanded;
        public static ItemTreasureDisk treasureDisk;

        public static ItemPrintout printout;

        public static ItemPeripheral peripheral;
        public static ItemAdvancedModem advancedModem;
        public static ItemCable cable;
        public static ItemBlock wiredModemFull;
    }

    public static class TurtleUpgrades
    {
        public static TurtleModem wirelessModem;
        public static TurtleModem advancedModem;
        public static TurtleSpeaker speaker;

        public static TurtleCraftingTable craftingTable;
        public static TurtleSword diamondSword;
        public static TurtleShovel diamondShovel;
        public static TurtleTool diamondPickaxe;
        public static TurtleAxe diamondAxe;
        public static TurtleHoe diamondHoe;
    }

    public static class PocketUpgrades
    {
        public static PocketModem wirelessModem;
        public static PocketModem advancedModem;
        public static PocketSpeaker speaker;

        @Deprecated
        public static PocketSpeaker pocketSpeaker;
    }

    @Deprecated
    public static class Upgrades
    {
        public static TurtleModem advancedModem;
    }

    // Registries
    public static final ClientComputerRegistry clientComputerRegistry = new ClientComputerRegistry();
    public static final ServerComputerRegistry serverComputerRegistry = new ServerComputerRegistry();

    // Creative
    public static CreativeTabMain mainCreativeTab;

    // Logging
    public static Logger log;

    // Peripheral providers. This is still here to ensure compatibility with Plethora and Computronics
    public static List<IPeripheralProvider> peripheralProviders = new ArrayList<>();

    // Implementation
    @Mod.Instance( value = ComputerCraft.MOD_ID )
    public static ComputerCraft instance;

    @SidedProxy(
        clientSide = "dan200.computercraft.client.proxy.ComputerCraftProxyClient",
        serverSide = "dan200.computercraft.shared.proxy.ComputerCraftProxyCommon"
    )
    private static IComputerCraftProxy proxy;

    @SidedProxy(
        clientSide = "dan200.computercraft.client.proxy.CCTurtleProxyClient",
        serverSide = "dan200.computercraft.shared.proxy.CCTurtleProxyCommon"
    )
    private static ICCTurtleProxy turtleProxy;

    @Mod.EventHandler
    public void preInit( FMLPreInitializationEvent event )
    {
        log = event.getModLog();

        // Load config
        Config.load( event.getSuggestedConfigurationFile() );

        // Setup network
        NetworkHandler.setup();

        proxy.preInit();
        turtleProxy.preInit();
    }

    @Mod.EventHandler
    public void init( FMLInitializationEvent event )
    {
        proxy.init();
        turtleProxy.init();
    }

    @Mod.EventHandler
    public void onServerStarting( FMLServerStartingEvent event )
    {
        proxy.initServer( event.getServer() );
    }

    @Mod.EventHandler
    public void onServerStart( FMLServerStartedEvent event )
    {
        if( FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            MainThread.reset();
            Tracking.reset();
        }
    }

    @Mod.EventHandler
    public void onServerStopped( FMLServerStoppedEvent event )
    {
        if( FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            MainThread.reset();
            Tracking.reset();
        }
    }

    public static String getVersion()
    {
        return "${version}";
    }

    public static void openDiskDriveGUI( EntityPlayer player, TileDiskDrive drive )
    {
        BlockPos pos = drive.getPos();
        player.openGui( ComputerCraft.instance, ComputerCraft.diskDriveGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openComputerGUI( EntityPlayer player, TileComputer computer )
    {
        BlockPos pos = computer.getPos();
        player.openGui( ComputerCraft.instance, ComputerCraft.computerGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openPrinterGUI( EntityPlayer player, TilePrinter printer )
    {
        BlockPos pos = printer.getPos();
        player.openGui( ComputerCraft.instance, ComputerCraft.printerGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openTurtleGUI( EntityPlayer player, TileTurtle turtle )
    {
        BlockPos pos = turtle.getPos();
        player.openGui( instance, ComputerCraft.turtleGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openPrintoutGUI( EntityPlayer player, EnumHand hand )
    {
        player.openGui( ComputerCraft.instance, ComputerCraft.printoutGUIID, player.getEntityWorld(), hand.ordinal(), 0, 0 );
    }

    public static void openPocketComputerGUI( EntityPlayer player, EnumHand hand )
    {
        player.openGui( ComputerCraft.instance, ComputerCraft.pocketComputerGUIID, player.getEntityWorld(), hand.ordinal(), 0, 0 );
    }

    public static void openComputerGUI( EntityPlayer player, ServerComputer computer )
    {
        ComputerFamily family = computer.getFamily();
        int width = 0, height = 0;
        Terminal terminal = computer.getTerminal();
        if( terminal != null )
        {
            width = terminal.getWidth();
            height = terminal.getHeight();
        }

        // Pack useful terminal information into the various coordinate bits.
        // These are extracted in ComputerCraftProxyCommon.getClientGuiElement
        player.openGui( ComputerCraft.instance, ComputerCraft.viewComputerGUIID, player.getEntityWorld(),
            computer.getInstanceID(), family.ordinal(), (width & 0xFFFF) << 16 | (height & 0xFFFF)
        );
    }

    private static File getBaseDir()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory();
    }

    private static File getResourcePackDir()
    {
        return new File( getBaseDir(), "resourcepacks" );
    }

    @Deprecated
    public static void registerPermissionProvider( ITurtlePermissionProvider provider )
    {
        TurtlePermissions.register( provider );
    }

    @Deprecated
    public static void registerPocketUpgrade( IPocketUpgrade upgrade )
    {
        dan200.computercraft.shared.PocketUpgrades.register( upgrade );
    }

    @Deprecated
    public static void registerPeripheralProvider( IPeripheralProvider provider )
    {
        Peripherals.register( provider );
    }

    @Deprecated
    public static void registerBundledRedstoneProvider( IBundledRedstoneProvider provider )
    {
        BundledRedstone.register( provider );
    }

    @Deprecated
    public static void registerMediaProvider( IMediaProvider provider )
    {
        MediaProviders.register( provider );
    }

    @Deprecated
    public static void registerAPIFactory( ILuaAPIFactory factory )
    {
        ApiFactories.register( factory );
    }

    @Deprecated
    public static IWiredNode createWiredNodeForElement( IWiredElement element )
    {
        return new WiredNode( element );
    }

    @Deprecated
    public static IWiredElement getWiredElementAt( IBlockAccess world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile != null && tile.hasCapability( CapabilityWiredElement.CAPABILITY, side )
            ? tile.getCapability( CapabilityWiredElement.CAPABILITY, side )
            : null;
    }

    @Deprecated
    public static int getDefaultBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        return BundledRedstone.getDefaultOutput( world, pos, side );
    }

    @Deprecated
    public static IPacketNetwork getWirelessNetwork()
    {
        return WirelessNetwork.getUniversal();
    }

    @Deprecated
    public static int createUniqueNumberedSaveDir( World world, String parentSubPath )
    {
        return IDAssigner.getNextIDFromDirectory( parentSubPath );
    }

    @Deprecated
    public static IWritableMount createSaveDirMount( World world, String subPath, long capacity )
    {
        try
        {
            return new FileMount( new File( getWorldDir(), subPath ), capacity );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Deprecated
    public static IMount createResourceMount( Class<?> modClass, String domain, String subPath )
    {
        // Start building list of mounts
        List<IMount> mounts = new ArrayList<>();
        subPath = "assets/" + domain + "/" + subPath;

        // Mount from debug dir
        File codeDir = getDebugCodeDir( modClass );
        if( codeDir != null )
        {
            File subResource = new File( codeDir, subPath );
            if( subResource.exists() )
            {
                IMount resourcePackMount = new FileMount( subResource, 0 );
                mounts.add( resourcePackMount );
            }
        }

        // Mount from mod jar
        File modJar = getContainingJar( modClass );
        if( modJar != null )
        {
            try
            {
                mounts.add( new JarMount( modJar, subPath ) );
            }
            catch( IOException | RuntimeException e )
            {
                ComputerCraft.log.error( "Could not load mount from mod jar", e );
            }
        }

        // Mount from resource packs
        File resourcePackDir = getResourcePackDir();
        if( resourcePackDir.exists() && resourcePackDir.isDirectory() )
        {
            String[] resourcePacks = resourcePackDir.list();
            for( String resourcePackName : resourcePacks )
            {
                try
                {
                    File resourcePack = new File( resourcePackDir, resourcePackName );
                    if( !resourcePack.isDirectory() )
                    {
                        // Mount a resource pack from a jar
                        mounts.add( new JarMount( resourcePack, subPath ) );
                    }
                    else
                    {
                        // Mount a resource pack from a folder
                        File subResource = new File( resourcePack, subPath );
                        if( subResource.exists() ) mounts.add( new FileMount( subResource, 0 ) );
                    }
                }
                catch( IOException | RuntimeException e )
                {
                    ComputerCraft.log.error( "Could not load resource pack '" + resourcePackName + "'", e );
                }
            }
        }

        // Return the combination of all the mounts found
        if( mounts.size() >= 2 )
        {
            IMount[] mountArray = new IMount[mounts.size()];
            mounts.toArray( mountArray );
            return new ComboMount( mountArray );
        }
        else if( mounts.size() == 1 )
        {
            return mounts.get( 0 );
        }
        else
        {
            return null;
        }
    }

    public static InputStream getResourceFile( Class<?> modClass, String domain, String subPath )
    {
        // Start searching in possible locations
        subPath = "assets/" + domain + "/" + subPath;

        // Look in resource packs
        File resourcePackDir = getResourcePackDir();
        if( resourcePackDir.exists() && resourcePackDir.isDirectory() )
        {
            String[] resourcePacks = resourcePackDir.list();
            for( String resourcePackPath : resourcePacks )
            {
                File resourcePack = new File( resourcePackDir, resourcePackPath );
                if( resourcePack.isDirectory() )
                {
                    // Mount a resource pack from a folder
                    File subResource = new File( resourcePack, subPath );
                    if( subResource.exists() && subResource.isFile() )
                    {
                        try
                        {
                            return new FileInputStream( subResource );
                        }
                        catch( FileNotFoundException ignored )
                        {
                        }
                    }
                }
                else
                {
                    ZipFile zipFile = null;
                    try
                    {
                        final ZipFile zip = zipFile = new ZipFile( resourcePack );
                        ZipEntry entry = zipFile.getEntry( subPath );
                        if( entry != null )
                        {
                            // Return a custom InputStream which will close the original zip when finished.
                            return new FilterInputStream( zipFile.getInputStream( entry ) )
                            {
                                @Override
                                public void close() throws IOException
                                {
                                    super.close();
                                    zip.close();
                                }
                            };
                        }
                        else
                        {
                            IoUtil.closeQuietly( zipFile );
                        }
                    }
                    catch( IOException e )
                    {
                        if( zipFile != null ) IoUtil.closeQuietly( zipFile );
                    }
                }
            }
        }

        // Look in debug dir
        File codeDir = getDebugCodeDir( modClass );
        if( codeDir != null )
        {
            File subResource = new File( codeDir, subPath );
            if( subResource.exists() && subResource.isFile() )
            {
                try
                {
                    return new FileInputStream( subResource );
                }
                catch( FileNotFoundException ignored )
                {
                }
            }
        }

        // Look in class loader
        return modClass.getClassLoader().getResourceAsStream( subPath );
    }

    private static File getContainingJar( Class<?> modClass )
    {
        String path = modClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        int bangIndex = path.indexOf( "!" );
        if( bangIndex >= 0 )
        {
            path = path.substring( 0, bangIndex );
        }

        URL url;
        try
        {
            url = new URL( path );
        }
        catch( MalformedURLException e1 )
        {
            return null;
        }

        File file;
        try
        {
            file = new File( url.toURI() );
        }
        catch( URISyntaxException e )
        {
            file = new File( url.getPath() );
        }
        return file;
    }

    private static File getDebugCodeDir( Class<?> modClass )
    {
        String path = modClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        int bangIndex = path.indexOf( "!" );
        return bangIndex >= 0 ? null : new File( new File( path ).getParentFile(), "../.." );
    }

    @Deprecated
    public static void registerTurtleUpgrade( ITurtleUpgrade upgrade )
    {
        dan200.computercraft.shared.TurtleUpgrades.register( upgrade );
    }

    public static File getWorldDir()
    {
        return DimensionManager.getCurrentSaveRootDirectory();
    }

    //region Compatibility
    @Deprecated
    public static File getWorldDir( World world )
    {
        return DimensionManager.getCurrentSaveRootDirectory();
    }

    @Deprecated
    public static IMedia getMedia( ItemStack stack )
    {
        return MediaProviders.get( stack );
    }

    @Deprecated
    public static IPocketUpgrade getPocketUpgrade( ItemStack stack )
    {
        return dan200.computercraft.shared.PocketUpgrades.get( stack );
    }

    @Deprecated
    public static ITurtleUpgrade getTurtleUpgrade( ItemStack stack )
    {
        return dan200.computercraft.shared.TurtleUpgrades.get( stack );
    }

    @Deprecated
    public static IPocketUpgrade getPocketUpgrade( String id )
    {
        return dan200.computercraft.shared.PocketUpgrades.get( id );
    }

    @Deprecated
    public static ITurtleUpgrade getTurtleUpgrade( String id )
    {
        return dan200.computercraft.shared.TurtleUpgrades.get( id );
    }

    @Deprecated
    public static IPeripheral getPeripheralAt( World world, BlockPos pos, EnumFacing side )
    {
        return Peripherals.getPeripheral( world, pos, side );
    }

    @Deprecated
    public static boolean canPlayerUseCommands( EntityPlayer player )
    {
        MinecraftServer server = player.getServer();
        return server != null && server.getPlayerList().canSendCommands( player.getGameProfile() );
    }
    //endregion
}
