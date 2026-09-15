// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.recipe;

import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.recipe.RecipeProperties;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

import java.util.function.Function;

/**
 * An abstract base class for creating recipes, in the style of {@link RecipeBuilder}.
 *
 * @param <S> The type of this class.
 * @param <O> The output of this builder.
 * @see ShapelessSpecBuilder
 */
public abstract class AbstractRecipeBuilder<S extends AbstractRecipeBuilder<S, O>, O> {
    protected final HolderGetter<Item> items;
    private final RecipeCategory category;
    protected final ItemStackTemplate result;
    private String group = "";
    private final RecipeUnlockAdvancementBuilder criteria = new RecipeUnlockAdvancementBuilder();

    protected AbstractRecipeBuilder(HolderGetter<Item> items, RecipeCategory category, ItemStackTemplate result) {
        this.items = items;
        this.category = category;
        this.result = result;
    }

    /**
     * Set the group for this recipe.
     *
     * @param group The new group.
     * @return This object, for chaining.
     */
    public final S group(String group) {
        this.group = group;
        return self();
    }

    /**
     * Add a criterion to this recipe.
     *
     * @param name      The name of the criterion.
     * @param criterion The criterion to add.
     * @return This object, for chaining.
     */
    public final S unlockedBy(String name, Criterion<?> criterion) {
        criteria.unlockedBy(name, criterion);
        return self();
    }

    /**
     * Convert this builder into the output ({@link O}) object.
     *
     * @param properties The properties for this recipe.
     * @return The built object.
     */
    protected abstract O build(RecipeProperties properties);

    /**
     * Convert this builder into a concrete recipe.
     *
     * @param factory The recipe's constructor.
     * @return The "built" recipe.
     */
    public final FinishedRecipe build(Function<O, Recipe<?>> factory) {
        var properties = new RecipeProperties(
            new CraftingRecipe.CraftingBookInfo(RecipeBuilder.determineCraftingBookCategory(category), group),
            new Recipe.CommonInfo(true)
        );
        return new FinishedRecipe(factory.apply(build(properties)), result, category, criteria);
    }

    /**
     * Convert this builder into a concrete recipe.
     *
     * @param factory The recipe's constructor.
     * @return The "built" recipe.
     */
    public final FinishedRecipe buildOrThrow(Function<O, DataResult<? extends Recipe<?>>> factory) {
        return build(s -> factory.apply(s).getOrThrow());
    }

    @SuppressWarnings("unchecked")
    private S self() {
        return (S) this;
    }

    public static final class FinishedRecipe {
        private final Recipe<?> recipe;
        private final ItemStackTemplate result;
        private final RecipeCategory category;
        private final RecipeUnlockAdvancementBuilder advancement;

        private FinishedRecipe(Recipe<?> recipe, ItemStackTemplate result, RecipeCategory category, RecipeUnlockAdvancementBuilder advancement) {
            this.recipe = recipe;
            this.result = result;
            this.category = category;
            this.advancement = advancement;
        }

        public void save(RecipeOutput output, Identifier id) {
            var key = recipeKey(id);
            output.accept(key, recipe, advancement.build(output, key, category));
        }

        public void save(RecipeOutput output) {
            save(output, RecipeBuilder.getDefaultRecipeId(result).identifier());
        }
    }

    protected static ResourceKey<Recipe<?>> recipeKey(Identifier key) {
        return ResourceKey.create(Registries.RECIPE, key);
    }
}
