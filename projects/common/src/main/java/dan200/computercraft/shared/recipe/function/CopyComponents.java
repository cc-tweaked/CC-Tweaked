// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link RecipeFunction} that copies components from one of the ingredients to the result.
 * <p>
 * This has the same behaviour as {@linkplain CopyComponentsFunction}, but operating within the scope of recipes.
 */
public final class CopyComponents implements RecipeFunction {
    public static final MapCodec<CopyComponents> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("from").forGetter(x -> x.from),
        DataComponentType.CODEC.listOf().optionalFieldOf("include").forGetter(x -> x.include),
        DataComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter(x -> x.exclude)
    ).apply(instance, CopyComponents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CopyComponents> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, x -> x.from,
        ByteBufCodecs.optional(DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.list())), x -> x.include,
        ByteBufCodecs.optional(DataComponentType.STREAM_CODEC.apply(ByteBufCodecs.list())), x -> x.exclude,
        CopyComponents::new
    );

    private final Ingredient from;
    private final Optional<List<DataComponentType<?>>> include;
    private final Optional<List<DataComponentType<?>>> exclude;

    private final @Nullable Set<DataComponentType<?>> includeSet;
    private final @Nullable Set<DataComponentType<?>> excludeSet;

    /**
     * Create a new {@link CopyComponents} that copies all components from an ingredient.
     *
     * @param from The ingredient to copy from.
     */
    public CopyComponents(Ingredient from) {
        this(from, Optional.empty(), Optional.empty());
    }

    /**
     * Create a new {@link CopyComponents} that copies all components from an ingredient.
     *
     * @param from The ingredient to copy from.
     */
    public CopyComponents(ItemLike from) {
        this(Ingredient.of(from));
    }

    private CopyComponents(Ingredient from, Optional<List<DataComponentType<?>>> include, Optional<List<DataComponentType<?>>> exclude) {
        this.from = from;
        this.include = include.map(List::copyOf);
        this.exclude = exclude.map(List::copyOf);

        includeSet = include.map(Set::copyOf).orElse(null);
        excludeSet = exclude.map(Set::copyOf).orElse(null);
    }

    @Override
    public Type<?> getType() {
        return ModRegistry.RecipeFunctions.COPY_COMPONENTS.get();
    }

    @Override
    public ItemStack apply(CraftingInput container, ItemStack result) {
        for (var item : container.items()) {
            if (from.test(item)) {
                applyPatch(item.getComponentsPatch(), result);
                break;
            }
        }

        return result;
    }

    private void applyPatch(DataComponentPatch patch, ItemStack result) {
        if (includeSet == null && excludeSet == null) {
            result.applyComponents(patch);
            return;
        }

        // Only apply components in the include set (if present) and not in the exclude set (if present).
        for (var component : patch.entrySet()) {
            var type = component.getKey();
            if ((includeSet == null || includeSet.contains(type)) && (excludeSet == null || !excludeSet.contains(type))) {
                unsafeSetComponent(result, type, component.getValue().orElse(null));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void unsafeSetComponent(ItemStack stack, DataComponentType<?> type, @Nullable T value) {
        stack.set((DataComponentType<T>) type, value);
    }

    /**
     * Create a new {@link CopyComponents} builder, that copies components from an ingredient.
     *
     * @param ingredient The ingredient to copy from.
     * @return The builder.
     */
    public static Builder builder(Ingredient ingredient) {
        return new Builder(ingredient);
    }

    /**
     * Create a new {@link CopyComponents} builder, that copies components from an ingredient.
     *
     * @param ingredient The ingredient to copy from.
     * @return The builder.
     */
    public static Builder builder(ItemLike ingredient) {
        return new Builder(Ingredient.of(ingredient));
    }

    public static final class Builder {
        private final Ingredient from;
        private @Nullable List<DataComponentType<?>> include;
        private @Nullable List<DataComponentType<?>> exclude;

        private Builder(Ingredient from) {
            this.from = from;
        }

        /**
         * Only copy the specified component.
         *
         * @param type The component to include.
         * @return {@code this}, for chaining.
         */
        public Builder include(DataComponentType<?> type) {
            if (this.include == null) include = new ArrayList<>();
            include.add(type);
            return this;
        }

        /**
         * Exclude a component from being copied.
         *
         * @param type The component to exclude.
         * @return {@code this}, for chaining.
         */
        public Builder exclude(DataComponentType<?> type) {
            if (exclude == null) exclude = new ArrayList<>();
            exclude.add(type);
            return this;
        }

        /**
         * Build the resulting {@link CopyComponents} instance.
         *
         * @return The constructed {@link CopyComponents} recipe function.
         */
        public CopyComponents build() {
            return new CopyComponents(from, Optional.ofNullable(include), Optional.ofNullable(exclude));
        }
    }
}
