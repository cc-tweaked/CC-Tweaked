/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenDoc;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.integration.crafttweaker.actions.AddTurtleTool;
import dan200.computercraft.shared.integration.crafttweaker.actions.RemoveTurtleUpgradeByItem;
import dan200.computercraft.shared.integration.crafttweaker.actions.RemoveTurtleUpgradeByName;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass( "dan200.computercraft.turtle" )
@ModOnly( ComputerCraft.MOD_ID )
public class TurtleTweaker
{
    @ZenMethod
    @ZenDoc( "Remove a turtle upgrade with the given id" )
    public static void removeUpgrade( String upgrade )
    {
        CraftTweakerAPI.apply( new RemoveTurtleUpgradeByName( upgrade ) );
    }

    @ZenMethod
    @ZenDoc( "Remove a turtle upgrade crafted with the given item stack" )
    public static void removeUpgrade( IItemStack stack )
    {
        CraftTweakerAPI.apply( new RemoveTurtleUpgradeByItem( CraftTweakerMC.getItemStack( stack ) ) );
    }

    @ZenMethod
    @ZenDoc( "Add a new turtle tool with the given id, which crafts and acts using the given stack." )
    public static void addTool( String id, IItemStack stack )
    {
        addTool( id, stack, stack, "tool" );
    }

    @ZenMethod
    @ZenDoc( "Add a new turtle tool with the given id, which is crafted with one item, and uses another." )
    public static void addTool( String id, IItemStack craftingStack, IItemStack toolStack )
    {
        addTool( id, craftingStack, toolStack, "tool" );
    }

    @ZenMethod
    @ZenDoc( "Add a new turtle tool with the given id, which crafts and acts using the given stack. You may also" +
        "specify a 'kind' of tool, which limits what blocks the turtle can break (for instance, an 'axe' may only break wood)." )
    public static void addTool( String id, IItemStack stack, String kind )
    {
        addTool( id, stack, stack, kind );
    }

    @ZenMethod
    @ZenDoc( "Add a new turtle tool with the given id, which is crafted with one item, and uses another. You may also" +
        "specify a 'kind' of tool, which limits what blocks the turtle can break (for instance, an 'axe' may only break wood)." )
    public static void addTool( String id, IItemStack craftingStack, IItemStack toolStack, String kind )
    {
        CraftTweakerAPI.apply( new AddTurtleTool( id, CraftTweakerMC.getItemStack( craftingStack ), CraftTweakerMC.getItemStack( toolStack ), kind ) );
    }
}
