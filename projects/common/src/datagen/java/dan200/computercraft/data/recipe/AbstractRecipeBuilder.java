// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.recipe;

import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.recipe.RecipeProperties;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * An abstract base class for creating recipes, in the style of {@link RecipeBuilder}.
 *
 * @param <S> The type of this class.
 * @param <O> The output of this builder.
 * @see ShapelessSpecBuilder
 */
public abstract class AbstractRecipeBuilder<S extends AbstractRecipeBuilder<S, O>, O> {
    private final RecipeCategory category;
    protected final ItemStack result;
    private String group = "";
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    protected AbstractRecipeBuilder(RecipeCategory category, ItemStack result) {
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
        criteria.put(name, criterion);
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
        var properties = new RecipeProperties(group, RecipeBuilder.determineBookCategory(category), true);
        return new FinishedRecipe(factory.apply(build(properties)), result.getItem(), category, criteria);
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
        private final Item result;
        private final RecipeCategory category;
        private final Map<String, Criterion<?>> criteria;

        private FinishedRecipe(Recipe<?> recipe, Item result, RecipeCategory category, Map<String, Criterion<?>> criteria) {
            this.recipe = recipe;
            this.result = result;
            this.category = category;
            this.criteria = criteria;
        }

        public void save(RecipeOutput output, ResourceLocation id) {
            if (criteria.isEmpty()) throw new IllegalStateException("No way of obtaining recipe " + id);
            var advancement = output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
            for (var entry : criteria.entrySet()) advancement.addCriterion(entry.getKey(), entry.getValue());

            output.accept(id, recipe, advancement.build(id.withPrefix("recipes/" + category.getFolderName() + "/")));
        }

        public void save(RecipeOutput output) {
            save(output, RecipeBuilder.getDefaultRecipeId(result));
        }
    }
}
