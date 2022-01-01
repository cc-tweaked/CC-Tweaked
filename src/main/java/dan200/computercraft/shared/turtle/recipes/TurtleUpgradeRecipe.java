/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.recipes;

import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public final class TurtleUpgradeRecipe extends CustomRecipe
{
    private TurtleUpgradeRecipe( ResourceLocation id )
    {
        super( id );
    }

    @Override
    public boolean canCraftInDimensions( int x, int y )
    {
        return x >= 3 && y >= 1;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem()
    {
        return TurtleItemFactory.create( -1, null, -1, ComputerFamily.NORMAL, null, null, 0, null );
    }

    @Override
    public boolean matches( @Nonnull CraftingContainer inventory, @Nonnull Level world )
    {
        return !assemble( inventory ).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingContainer inventory )
    {
        // Scan the grid for a row containing a turtle and 1 or 2 items
        ItemStack leftItem = ItemStack.EMPTY;
        ItemStack turtle = ItemStack.EMPTY;
        ItemStack rightItem = ItemStack.EMPTY;

        for( int y = 0; y < inventory.getHeight(); y++ )
        {
            if( turtle.isEmpty() )
            {
                // Search this row for potential turtles
                boolean finishedRow = false;
                for( int x = 0; x < inventory.getWidth(); x++ )
                {
                    ItemStack item = inventory.getItem( x + y * inventory.getWidth() );
                    if( !item.isEmpty() )
                    {
                        if( finishedRow )
                        {
                            return ItemStack.EMPTY;
                        }

                        if( item.getItem() instanceof ITurtleItem )
                        {
                            // Item is a turtle
                            if( turtle.isEmpty() )
                            {
                                turtle = item;
                            }
                            else
                            {
                                return ItemStack.EMPTY;
                            }
                        }
                        else
                        {
                            // Item is not a turtle
                            if( turtle.isEmpty() && leftItem.isEmpty() )
                            {
                                leftItem = item;
                            }
                            else if( !turtle.isEmpty() && rightItem.isEmpty() )
                            {
                                rightItem = item;
                            }
                            else
                            {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                    else
                    {
                        // Item is empty
                        if( !leftItem.isEmpty() || !turtle.isEmpty() )
                        {
                            finishedRow = true;
                        }
                    }
                }

                // If we found anything, check we found a turtle too
                if( turtle.isEmpty() && (!leftItem.isEmpty() || !rightItem.isEmpty()) )
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // Turtle is already found, just check this row is empty
                for( int x = 0; x < inventory.getWidth(); x++ )
                {
                    ItemStack item = inventory.getItem( x + y * inventory.getWidth() );
                    if( !item.isEmpty() )
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // See if we found a turtle + one or more items
        if( turtle.isEmpty() || leftItem.isEmpty() && rightItem.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        // At this point we have a turtle + 1 or 2 items
        // Get the turtle we already have
        ITurtleItem itemTurtle = (ITurtleItem) turtle.getItem();
        ComputerFamily family = itemTurtle.getFamily();
        ITurtleUpgrade[] upgrades = new ITurtleUpgrade[] {
            itemTurtle.getUpgrade( turtle, TurtleSide.LEFT ),
            itemTurtle.getUpgrade( turtle, TurtleSide.RIGHT ),
        };

        // Get the upgrades for the new items
        ItemStack[] items = new ItemStack[] { rightItem, leftItem };
        for( int i = 0; i < 2; i++ )
        {
            if( !items[i].isEmpty() )
            {
                ITurtleUpgrade itemUpgrade = TurtleUpgrades.instance().get( items[i] );
                if( itemUpgrade == null || upgrades[i] != null ) return ItemStack.EMPTY;
                upgrades[i] = itemUpgrade;
            }
        }

        // Construct the new stack
        int computerID = itemTurtle.getComputerID( turtle );
        String label = itemTurtle.getLabel( turtle );
        int fuelLevel = itemTurtle.getFuelLevel( turtle );
        int colour = itemTurtle.getColour( turtle );
        ResourceLocation overlay = itemTurtle.getOverlay( turtle );
        return TurtleItemFactory.create( computerID, label, colour, family, upgrades[0], upgrades[1], fuelLevel, overlay );
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final SimpleRecipeSerializer<TurtleUpgradeRecipe> SERIALIZER = new SimpleRecipeSerializer<>( TurtleUpgradeRecipe::new );
}
