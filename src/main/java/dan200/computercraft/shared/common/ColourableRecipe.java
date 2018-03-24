package dan200.computercraft.shared.common;

import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;

public class ColourableRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe
{
    @Override
    public boolean matches( @Nonnull InventoryCrafting inv, @Nonnull World worldIn )
    {
        boolean hasColourable = false;
        boolean hasDye = false;
        for( int i = 0; i < inv.getSizeInventory(); i++ )
        {
            ItemStack stack = inv.getStackInSlot( i );
            if( stack.isEmpty() ) continue;

            if( stack.getItem() instanceof IColouredItem )
            {
                if( hasColourable ) return false;
                hasColourable = true;
            }
            else if( ColourUtils.getStackColour( stack ) >= 0 )
            {
                hasDye = true;
            }
            else
            {
                return false;
            }
        }

        return hasColourable && hasDye;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull InventoryCrafting inv )
    {
        ItemStack colourable = ItemStack.EMPTY;

        ColourTracker tracker = new ColourTracker();

        for( int i = 0; i < inv.getSizeInventory(); ++i )
        {
            ItemStack stack = inv.getStackInSlot( i );

            if( stack.isEmpty() ) continue;

            if( stack.getItem() instanceof IColouredItem )
            {
                colourable = stack;
            }
            else
            {
                int index = ColourUtils.getStackColour( stack );
                if( index < 0 ) continue;

                Colour colour = Colour.values()[ index ];
                tracker.addColour( colour.getR(), colour.getG(), colour.getB() );
            }
        }

        if( colourable.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        return ((IColouredItem) colourable.getItem()).setColour( colourable, tracker.getColour() );
    }

    @Override
    public boolean canFit( int x, int y )
    {
        return x >= 2 && y >= 2;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getRecipeOutput()
    {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public NonNullList<ItemStack> getRemainingItems( @Nonnull InventoryCrafting inventoryCrafting )
    {
        NonNullList<ItemStack> results = NonNullList.withSize( inventoryCrafting.getSizeInventory(), ItemStack.EMPTY );
        for( int i = 0; i < results.size(); ++i )
        {
            ItemStack stack = inventoryCrafting.getStackInSlot( i );
            results.set( i, ForgeHooks.getContainerItem( stack ) );
        }
        return results;
    }
}
