/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityCableRenderer;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class ComputerCraftProxyClient extends ComputerCraftProxyCommon
{
    @Override
    public void preInit()
    {
        super.preInit();

        // Register any client-specific commands
        ClientCommandHandler.instance.registerCommand( CommandCopy.INSTANCE );
    }

    @Override
    public void init()
    {
        super.init();

        Minecraft mc = Minecraft.getMinecraft();

        // Setup
        mc.getItemColors().registerItemColorHandler( new DiskColorHandler( ComputerCraft.Items.disk ), ComputerCraft.Items.disk );
        mc.getItemColors().registerItemColorHandler( new DiskColorHandler( ComputerCraft.Items.diskExpanded ), ComputerCraft.Items.diskExpanded );

        mc.getItemColors().registerItemColorHandler( ( stack, layer ) -> {
            switch( layer )
            {
                case 0:
                default:
                    return 0xFFFFFF;
                case 1:
                {
                    // Frame colour
                    int colour = ComputerCraft.Items.pocketComputer.getColour( stack );
                    return colour == -1 ? 0xFFFFFF : colour;
                }
                case 2:
                {
                    // Light colour
                    int colour = ComputerCraft.Items.pocketComputer.getLightState( stack );
                    return colour == -1 ? Colour.Black.getHex() : colour;
                }
            }
        }, ComputerCraft.Items.pocketComputer );

        // Setup renderers
        ClientRegistry.bindTileEntitySpecialRenderer( TileMonitor.class, new TileEntityMonitorRenderer() );
        ClientRegistry.bindTileEntitySpecialRenderer( TileCable.class, new TileEntityCableRenderer() );
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Side.CLIENT )
    public static class ForgeHandlers
    {
        @SubscribeEvent
        public static void onWorldUnload( WorldEvent.Unload event )
        {
            if( event.getWorld().isRemote )
            {
                ClientMonitor.destroyAll();
            }
        }
    }

    @SideOnly( Side.CLIENT )
    private static class DiskColorHandler implements IItemColor
    {
        private final ItemDiskLegacy disk;

        private DiskColorHandler( ItemDiskLegacy disk )
        {
            this.disk = disk;
        }

        @Override
        public int colorMultiplier( @Nonnull ItemStack stack, int layer )
        {
            return layer == 0 ? 0xFFFFFF : disk.getColour( stack );
        }
    }
}
