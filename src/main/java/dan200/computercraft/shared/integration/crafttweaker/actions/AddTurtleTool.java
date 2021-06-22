/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker.actions;

import com.blamejared.crafttweaker.api.actions.IUndoableAction;
import com.blamejared.crafttweaker.api.logger.ILogger;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.integration.crafttweaker.TrackingLogger;
import dan200.computercraft.shared.turtle.upgrades.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.Map;

/**
 * Register a new turtle tool.
 */
public class AddTurtleTool implements IUndoableAction
{
    private interface Factory
    {
        TurtleTool create( ResourceLocation location, ItemStack craftItem, ItemStack toolItem );
    }

    private static final Map<String, Factory> kinds = new HashMap<>();

    static
    {
        kinds.put( "tool", TurtleTool::new );
        kinds.put( "axe", TurtleAxe::new );
        kinds.put( "hoe", TurtleHoe::new );
        kinds.put( "shovel", TurtleShovel::new );
        kinds.put( "sword", TurtleSword::new );
    }

    private final String id;
    private final ItemStack craftItem;
    private final ItemStack toolItem;
    private final String kind;

    private ITurtleUpgrade upgrade;

    public AddTurtleTool( String id, ItemStack craftItem, ItemStack toolItem, String kind )
    {
        this.id = id;
        this.craftItem = craftItem;
        this.toolItem = toolItem;
        this.kind = kind;
    }

    @Override
    public void apply()
    {
        ITurtleUpgrade upgrade = this.upgrade;
        if( upgrade == null )
        {
            Factory factory = kinds.get( kind );
            if( factory == null )
            {
                ComputerCraft.log.error( "Unknown turtle upgrade kind '{}' (this should have been rejected by verify!)", kind );
                return;
            }

            upgrade = this.upgrade = factory.create( new ResourceLocation( id ), craftItem, toolItem );
        }

        try
        {
            TurtleUpgrades.register( upgrade );
        }
        catch( RuntimeException e )
        {
            ComputerCraft.log.error( "Registration of turtle tool failed", e );
        }
    }

    @Override
    public String describe()
    {
        return String.format( "Add new turtle %s '%s' (crafted with '%s', uses a '%s')", kind, id, craftItem, toolItem );
    }

    @Override
    public void undo()
    {
        if( upgrade != null ) TurtleUpgrades.remove( upgrade );
    }

    @Override
    public String describeUndo()
    {
        return String.format( "Removing turtle upgrade %s.", id );
    }

    @Override
    public boolean validate( ILogger logger )
    {
        TrackingLogger trackLog = new TrackingLogger( logger );

        if( craftItem.isEmpty() ) trackLog.error( "Crafting item stack is empty." );

        if( craftItem.isDamaged() || craftItem.isEnchanted() || craftItem.hasCustomHoverName() )
        {
            trackLog.warning( "Crafting item has NBT." );
        }
        if( toolItem.isEmpty() ) trackLog.error( "Tool item stack is empty." );

        if( !kinds.containsKey( kind ) ) trackLog.error( String.format( "Unknown kind '%s'.", kind ) );

        if( TurtleUpgrades.get( id ) != null )
        {
            trackLog.error( String.format( "An upgrade with the same name ('%s') has already been registered.", id ) );
        }

        return trackLog.isOk();
    }

    @Override
    public boolean shouldApplyOn( LogicalSide side )
    {
        return shouldApplySingletons();
    }
}
