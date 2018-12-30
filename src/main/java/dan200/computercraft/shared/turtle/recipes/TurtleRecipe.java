/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.recipes;

import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.recipe.ComputerConvertRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.RecipeUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

import javax.annotation.Nonnull;

public class TurtleRecipe extends ComputerConvertRecipe
{
    private final ComputerFamily family;

    public TurtleRecipe( String group, @Nonnull CraftingHelper.ShapedPrimer primer, ComputerFamily family )
    {
        super( group, primer, TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null ) );
        this.family = family;
    }

    @Nonnull
    @Override
    protected ItemStack convert( IComputerItem item, @Nonnull ItemStack stack )
    {
        int computerID = item.getComputerID( stack );
        String label = item.getLabel( stack );

        return TurtleItemFactory.create( computerID, label, -1, family, null, null, 0, null );
    }

    public static class Factory implements IRecipeFactory
    {
        @Override
        public IRecipe parse( JsonContext context, JsonObject json )
        {
            String group = JsonUtils.getString( json, "group", "" );
            ComputerFamily family = RecipeUtil.getFamily( json, "family" );
            CraftingHelper.ShapedPrimer primer = RecipeUtil.getPrimer( context, json );

            return new TurtleRecipe( group, primer, family );
        }
    }
}
