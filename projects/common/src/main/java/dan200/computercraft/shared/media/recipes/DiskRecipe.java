// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.recipe.AbstractCraftingRecipe;
import dan200.computercraft.shared.recipe.RecipeProperties;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DiskRecipe extends AbstractCraftingRecipe {
    public static final MapCodec<DiskRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeProperties.CODEC.forGetter(x -> x.properties),
        ShapelessRecipeSpec.INGREDIENT_CODEC.fieldOf("ingredients").forGetter(x -> x.ingredients)
    ).apply(instance, DiskRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskRecipe> STREAM_CODEC = StreamCodec.composite(
        RecipeProperties.STREAM_CODEC, x -> x.properties,
        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), x -> x.ingredients,
        DiskRecipe::new
    );

    private final List<Ingredient> ingredients;
    private @Nullable PlacementInfo placementInfo;

    public DiskRecipe(RecipeProperties properties, List<Ingredient> ingredients) {
        super(properties);
        this.ingredients = ingredients;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) placementInfo = PlacementInfo.create(ingredients);
        return placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        var dyes = ColourUtils.DYES;
        List<RecipeDisplay> out = new ArrayList<>(dyes.size());
        for (var i = 0; i < dyes.size(); i++) {
            var tracker = new ColourTracker();
            tracker.addColour(DyeColor.byId(i));

            out.add(new ShapelessCraftingRecipeDisplay(
                Stream.concat(ingredients.stream(), Stream.of(Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(dyes.get(i)))))
                    .map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(DataComponentUtil.createStack(
                    ModRegistry.Items.DISK.get(), DataComponents.DYED_COLOR, new DyedItemColor(tracker.getColour(), false)
                )),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            ));
        }
        return out;
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        var inputs = 0;
        var stackedContents = new StackedItemContents();

        for (var i = 0; i < inv.size(); ++i) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (ColourUtils.getStackColour(stack) == null) {
                stackedContents.accountStack(stack, 1);
            }
        }

        return inputs == ingredients.size() && stackedContents.canCraft(placementInfo().unpackedIngredients(), null);
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registryAccess) {
        var tracker = new ColourTracker();

        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            var dye = ColourUtils.getStackColour(stack);
            if (dye != null) tracker.addColour(dye);
        }

        return DataComponentUtil.createStack(ModRegistry.Items.DISK.get(), DataComponents.DYED_COLOR, new DyedItemColor(tracker.hasColour() ? tracker.getColour() : Colour.BLUE.getHex(), false));
    }

    @Override
    public RecipeSerializer<DiskRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.DISK.get();
    }
}
