package dan200.computercraft.shared.computer.recipe;

import com.google.gson.JsonObject;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.util.RecipeUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

import javax.annotation.Nonnull;

public class ComputerFamilyRecipe extends ComputerConvertRecipe
{
    private final ComputerFamily family;

    public ComputerFamilyRecipe( String group, @Nonnull CraftingHelper.ShapedPrimer primer, @Nonnull ItemStack result, ComputerFamily family )
    {
        super( group, primer, result );
        this.family = family;
    }

    @Nonnull
    @Override
    protected ItemStack convert( IComputerItem item, @Nonnull ItemStack stack )
    {
        return item.withFamily( stack, family );
    }

    public static class Factory implements IRecipeFactory
    {
        @Override
        public IRecipe parse( JsonContext context, JsonObject json )
        {
            String group = JsonUtils.getString( json, "group", "" );
            ComputerFamily family = RecipeUtil.getFamily( json, "family" );

            CraftingHelper.ShapedPrimer primer = RecipeUtil.getPrimer( context, json );
            ItemStack result = deserializeItem( JsonUtils.getJsonObject( json, "result" ), false );

            return new ComputerFamilyRecipe( group, primer, result, family );
        }
    }
}
