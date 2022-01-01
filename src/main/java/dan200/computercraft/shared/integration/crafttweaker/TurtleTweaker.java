/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;
import dan200.computercraft.shared.integration.crafttweaker.actions.AddTurtleTool;
import dan200.computercraft.shared.integration.crafttweaker.actions.RemoveTurtleUpgradeByItem;
import dan200.computercraft.shared.integration.crafttweaker.actions.RemoveTurtleUpgradeByName;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name( "dan200.computercraft.turtle" )
public class TurtleTweaker
{
    /**
     * Remove a turtle upgrade with the given id.
     *
     * @param upgrade The ID of the to remove
     */
    @ZenCodeType.Method
    public static void removeUpgrade( String upgrade )
    {
        CraftTweakerAPI.apply( new RemoveTurtleUpgradeByName( upgrade ) );
    }

    /**
     * Remove a turtle upgrade crafted with the given item stack".
     *
     * @param stack The stack with which the upgrade is crafted.
     */
    @ZenCodeType.Method
    public static void removeUpgrade( IItemStack stack )
    {
        CraftTweakerAPI.apply( new RemoveTurtleUpgradeByItem( stack.getInternal() ) );
    }

    /**
     * Add a new turtle tool with the given id, which crafts and acts using the given stack.
     *
     * @param id    The new upgrade's ID
     * @param stack The stack used for crafting the upgrade and used by the turtle as a tool.
     */
    @ZenCodeType.Method
    public static void addTool( String id, IItemStack stack )
    {
        addTool( id, stack, stack, "tool" );
    }

    @ZenCodeType.Method
    public static void addTool( String id, IItemStack craftingStack, IItemStack toolStack )
    {
        addTool( id, craftingStack, toolStack, "tool" );
    }

    @ZenCodeType.Method
    public static void addTool( String id, IItemStack stack, String kind )
    {
        addTool( id, stack, stack, kind );
    }

    @ZenCodeType.Method
    public static void addTool( String id, IItemStack craftingStack, IItemStack toolStack, String kind )
    {
        CraftTweakerAPI.apply( new AddTurtleTool( id, craftingStack.getInternal(), toolStack.getInternal(), kind ) );
    }
}
