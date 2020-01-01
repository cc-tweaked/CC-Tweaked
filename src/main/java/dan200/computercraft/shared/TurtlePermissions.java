/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.permissions.ITurtlePermissionProvider;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class TurtlePermissions
{
    private static final Collection<ITurtlePermissionProvider> providers = new LinkedHashSet<>();

    private TurtlePermissions()
    {
    }

    public static void register( @Nonnull ITurtlePermissionProvider upgrade )
    {
        Objects.requireNonNull( upgrade, "upgrade cannot be null" );

        providers.add( upgrade );
    }

    public static boolean isBlockEnterable( World world, BlockPos pos, EntityPlayer player )
    {
        MinecraftServer server = player.getServer();
        if( server != null && !world.isRemote && server.isBlockProtected( world, pos, player ) )
        {
            return false;
        }

        for( ITurtlePermissionProvider provider : providers )
        {
            if( !provider.isBlockEnterable( world, pos ) ) return false;
        }
        return true;
    }

    public static boolean isBlockEditable( World world, BlockPos pos, EntityPlayer player )
    {
        MinecraftServer server = player.getServer();
        if( server != null && !world.isRemote && server.isBlockProtected( world, pos, player ) )
        {
            return false;
        }

        for( ITurtlePermissionProvider provider : providers )
        {
            if( !provider.isBlockEditable( world, pos ) ) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onTurtleAction( TurtleActionEvent event )
    {
        if( ComputerCraft.turtleDisabledActions.contains( event.getAction() ) )
        {
            event.setCanceled( true, "Action has been disabled" );
        }
    }
}
