// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.recipe.RecipeProperties;
import dan200.computercraft.shared.recipe.ShapelessRecipeSpec;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DiskRecipe extends NormalCraftingRecipe {
    public static final MapCodec<DiskRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeProperties.CODEC.forGetter(RecipeProperties::of),
        ShapelessRecipeSpec.INGREDIENT_CODEC.fieldOf("ingredients").forGetter(x -> x.ingredients)
    ).apply(instance, DiskRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskRecipe> STREAM_CODEC = StreamCodec.composite(
        RecipeProperties.STREAM_CODEC, RecipeProperties::of,
        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), x -> x.ingredients,
        DiskRecipe::new
    );

    public static final RecipeSerializer<DiskRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private final List<Ingredient> ingredients;

    public DiskRecipe(RecipeProperties properties, List<Ingredient> ingredients) {
        super(properties.common(), properties.book());
        this.ingredients = ingredients;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(ingredients);
    }

    @Override
    public List<RecipeDisplay> display() {
        var dyes = ColourUtils.DYES;
        List<RecipeDisplay> out = new ArrayList<>(dyes.size());
        for (var i = 0; i < dyes.size(); i++) {
            out.add(new ShapelessCraftingRecipeDisplay(
                Stream.concat(ingredients.stream(), Stream.of(Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(dyes.get(i)))))
                    .map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(ModRegistry.Items.DISK.get(), DataComponentPatch.builder()
                    .set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.byId(i).getTextureDiffuseColor()))
                    .build())),
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
            if (!ColourUtils.isDye(stack)) {
                inputs++;
                stackedContents.accountStack(stack, 1);
            }
        }

        return inputs == ingredients.size() && stackedContents.canCraft(placementInfo().ingredients(), null);
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        var tracker = new ColourTracker();

        for (var i = 0; i < inv.size(); i++) {
            var stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            var dye = ColourUtils.getDyeColour(stack);
            if (dye != null) tracker.addColour(dye);
        }

        return DataComponentUtil.createDyedStack(ModRegistry.Items.DISK.get(), tracker.getColourOr(Colour.BLUE.getHex()));
    }

    @Override
    public RecipeSerializer<DiskRecipe> getSerializer() {
        return SERIALIZER;
    }
}
