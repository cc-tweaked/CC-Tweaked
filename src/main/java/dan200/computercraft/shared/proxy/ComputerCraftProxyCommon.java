/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.shared.Config;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.ContainerViewComputer;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.*;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.datafix.Fixes;
import dan200.computercraft.shared.integration.charset.IntegrationCharset;
import dan200.computercraft.shared.media.common.DefaultMediaProvider;
import dan200.computercraft.shared.media.inventory.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheralProvider;
import dan200.computercraft.shared.peripheral.common.DefaultPeripheralProvider;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.asie.charset.ModCharset;

public class ComputerCraftProxyCommon implements IComputerCraftProxy
{
    @Override
    public void preInit()
    {
        // Creative tab
        ComputerCraft.mainCreativeTab = new CreativeTabMain( CreativeTabs.getNextID() );
    }

    @Override
    public void init()
    {
        registerProviders();
        NetworkRegistry.INSTANCE.registerGuiHandler( ComputerCraft.instance, new GuiHandler() );

        Fixes.register( FMLCommonHandler.instance().getDataFixer() );
        if( Loader.isModLoaded( ModCharset.MODID ) ) IntegrationCharset.register();
    }

    @Override
    public void initServer( MinecraftServer server )
    {
        CommandHandler handler = (CommandHandler) server.getCommandManager();
        handler.registerCommand( new CommandComputerCraft() );
    }

    private void registerProviders()
    {
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

    public class GuiHandler implements IGuiHandler
    {
        private GuiHandler()
        {
        }

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
        @SideOnly( Side.CLIENT )
        public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
        {
            BlockPos pos = new BlockPos( x, y, z );
            switch( id )
            {
                case ComputerCraft.diskDriveGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    return tile instanceof TileDiskDrive ? new GuiDiskDrive( new ContainerDiskDrive( player.inventory, (TileDiskDrive) tile ) ) : null;
                }
                case ComputerCraft.computerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    return tile instanceof TileComputer ? new GuiComputer( (TileComputer) tile ) : null;
                }
                case ComputerCraft.printerGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    return tile instanceof TilePrinter ? new GuiPrinter( new ContainerPrinter( player.inventory, (TilePrinter) tile ) ) : null;
                }
                case ComputerCraft.turtleGUIID:
                {
                    TileEntity tile = world.getTileEntity( pos );
                    if( tile instanceof TileTurtle )
                    {
                        TileTurtle turtle = (TileTurtle) tile;
                        return new GuiTurtle( turtle, new ContainerTurtle( player.inventory, turtle.getAccess() ) );
                    }
                    return null;
                }
                case ComputerCraft.printoutGUIID:
                {
                    ContainerHeldItem container = new ContainerHeldItem( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                    return container.getStack().getItem() instanceof ItemPrintout ? new GuiPrintout( container ) : null;
                }
                case ComputerCraft.pocketComputerGUIID:
                {
                    ContainerPocketComputer container = new ContainerPocketComputer( player, x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND );
                    return container.getStack().getItem() instanceof ItemPocketComputer ? new GuiPocketComputer( container ) : null;
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

                    ContainerViewComputer container = new ContainerViewComputer( computer );
                    return new GuiComputer( container, family, computer, width, height );
                }
                default:
                    return null;
            }
        }
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
    public final static class ForgeHandlers
    {
        private ForgeHandlers()
        {
        }

        @SubscribeEvent
        public static void onConnectionOpened( FMLNetworkEvent.ClientConnectedToServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public static void onConnectionClosed( FMLNetworkEvent.ClientDisconnectionFromServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public static void onClientTick( TickEvent.ClientTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
            {
                ComputerCraft.clientComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public static void onServerTick( TickEvent.ServerTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
            {
                MainThread.executePendingTasks();
                ComputerCraft.serverComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public static void onConfigChanged( ConfigChangedEvent.OnConfigChangedEvent event )
        {
            if( event.getModID().equals( ComputerCraft.MOD_ID ) ) Config.sync();
        }

        @SubscribeEvent
        public static void onContainerOpen( PlayerContainerEvent.Open event )
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
