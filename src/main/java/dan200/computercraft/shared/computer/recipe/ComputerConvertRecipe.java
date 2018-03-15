package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nonnull;

/**
 * Represents a recipe which converts a computer from one form into another.
 */
public abstract class ComputerConvertRecipe extends ShapedRecipes
{
    public ComputerConvertRecipe( String group, @Nonnull CraftingHelper.ShapedPrimer primer, @Nonnull ItemStack result )
    {
        super( group, primer.width, primer.height, primer.input, result );
    }

    @Nonnull
    protected abstract ItemStack convert( @Nonnull ItemStack stack );

    @Override
    public boolean matches( @Nonnull InventoryCrafting inventory, @Nonnull World world )
    {
        // See if we match the recipe, and extract the input computercraft ID
        ItemStack computerStack = null;
        for( int y = 0; y < 3; ++y )
        {
            for( int x = 0; x < 3; ++x )
            {
                ItemStack stack = inventory.getStackInRowAndColumn( x, y );
                Ingredient target = getIngredients().get( x + y * 3 );

                // First verify we match the ingredient
                if( !target.apply( stack ) ) return false;

                // We want to ensure we have a computer item somewhere in the recipe
                if( stack.getItem() instanceof IComputerItem ) computerStack = stack;
            }
        }

        return computerStack != null;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull InventoryCrafting inventory )
    {
        for( int y = 0; y < 3; ++y )
        {
            for( int x = 0; x < 3; ++x )
            {
                ItemStack item = inventory.getStackInRowAndColumn( x, y );

                // If we're a computer, convert!
                if( item.getItem() instanceof IComputerItem ) return convert( item );
            }
        }

        return ItemStack.EMPTY;
    }
}
