/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public class DebugOverlay
{
    @SubscribeEvent
    public static void onRenderText( RenderGameOverlayEvent.Text event )
    {
        Minecraft minecraft = Minecraft.getInstance();
        if( !minecraft.options.renderDebug || minecraft.level == null ) return;
        if( minecraft.hitResult == null || minecraft.hitResult.getType() != RayTraceResult.Type.BLOCK ) return;

        TileEntity tile = minecraft.level.getBlockEntity( ((BlockRayTraceResult) minecraft.hitResult).getBlockPos() );

        if( tile instanceof TileMonitor )
        {
            TileMonitor monitor = (TileMonitor) tile;
            event.getRight().add( "" );
            event.getRight().add(
                String.format( "Targeted monitor: (%d, %d), %d x %d", monitor.getXIndex(), monitor.getYIndex(), monitor.getWidth(), monitor.getHeight() )
            );
        }
        else if( tile instanceof TileTurtle )
        {
            TileTurtle turtle = (TileTurtle) tile;

            event.getRight().add( "" );
            event.getRight().add( "Targeted turtle:" );
            event.getRight().add( String.format( "Id: %d", turtle.getComputerID() ) );
            addTurtleUpgrade( event.getRight(), turtle, TurtleSide.LEFT );
            addTurtleUpgrade( event.getRight(), turtle, TurtleSide.RIGHT );
        }
    }

    private static void addTurtleUpgrade( List<String> out, TileTurtle turtle, TurtleSide side )
    {
        ITurtleUpgrade upgrade = turtle.getUpgrade( side );
        if( upgrade != null ) out.add( String.format( "Upgrade[%s]: %s", side, upgrade.getUpgradeID() ) );
    }
}
