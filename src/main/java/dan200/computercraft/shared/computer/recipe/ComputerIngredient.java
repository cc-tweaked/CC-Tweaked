package dan200.computercraft.shared.computer.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.util.RecipeUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents an ingredient which requires an item to have a specific
 * computer family. This allows us to have operations which only work
 * on normal or
 */
public class ComputerIngredient extends Ingredient
{
    private final IComputerItem item;
    private final ComputerFamily family;

    public <T extends Item & IComputerItem> ComputerIngredient( T item, int data, ComputerFamily family )
    {
        super( new ItemStack( item, 1, data ) );

        this.item = item;
        this.family = family;
    }

    @Override
    public boolean apply( @Nullable ItemStack stack )
    {
        return stack != null && stack.getItem() == item && item.getFamily( stack ) == family;
    }

    public static class Factory implements IIngredientFactory
    {
        @Nonnull
        @Override
        public Ingredient parse( JsonContext context, JsonObject json )
        {
            String itemName = context.appendModId( JsonUtils.getString( json, "item" ) );
            int data = JsonUtils.getInt( json, "data", 0 );
            ComputerFamily family = RecipeUtil.getFamily( json, "family" );

            Item item = ForgeRegistries.ITEMS.getValue( new ResourceLocation( itemName ) );

            if( item == null ) throw new JsonSyntaxException( "Unknown item '" + itemName + "'" );
            if( !(item instanceof IComputerItem) )
            {
                throw new JsonSyntaxException( "Item '" + itemName + "' is not a computer item" );
            }


            return new ComputerIngredient( (Item & IComputerItem) item, data, family );
        }
    }
}
