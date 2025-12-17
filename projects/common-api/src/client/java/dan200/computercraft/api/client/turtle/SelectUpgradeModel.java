// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link TurtleUpgradeModel} which selects between different models based on the value of a component in
 * {@linkplain UpgradeData#data() the upgrade's data}.
 * <p>
 * This is the {@link TurtleUpgradeModel} equivalent of {@link SelectItemModel}.
 *
 * @param <T> The type of value to switch on.
 */
public final class SelectUpgradeModel<T> implements TurtleUpgradeModel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "select");
    public static final MapCodec<? extends TurtleUpgradeModel.Unbaked> CODEC = RecordCodecBuilder.<Unbaked<?>>mapCodec(instance -> instance.group(
        Cases.CODEC.forGetter(Unbaked::cases),
        TurtleUpgradeModel.CODEC.optionalFieldOf("fallback").forGetter(Unbaked::fallback)
    ).apply(instance, Unbaked::new));

    private final DataComponentType<T> component;
    private final Map<T, TurtleUpgradeModel> cases;
    private final TurtleUpgradeModel fallback;

    private SelectUpgradeModel(DataComponentType<T> component, Map<T, TurtleUpgradeModel> cases, TurtleUpgradeModel fallback) {
        this.component = component;
        this.cases = cases;
        this.fallback = fallback;
    }

    private TurtleUpgradeModel getModel(UpgradeData<ITurtleUpgrade> upgrade) {
        var value = upgrade.get(component);
        if (value == null) return fallback;

        var model = cases.get(value);
        return model != null ? model : fallback;
    }

    @Override
    public void renderForItem(UpgradeData<ITurtleUpgrade> upgrade, TurtleSide side, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed) {
        getModel(upgrade).renderForItem(upgrade, side, renderer, resolver, transform, seed);
    }

    private record Unbaked<T>(
        Cases<T> cases,
        Optional<TurtleUpgradeModel.Unbaked> fallback
    ) implements TurtleUpgradeModel.Unbaked {
        private static final TurtleUpgradeModel.Unbaked MISSING = BasicUpgradeModel.unbaked(MissingBlockModel.LOCATION, MissingBlockModel.LOCATION);

        @Override
        public MapCodec<? extends TurtleUpgradeModel.Unbaked> type() {
            return CODEC;
        }

        @Override
        public TurtleUpgradeModel bake(ModelBaker baker) {
            Map<T, TurtleUpgradeModel> cases = new Object2ObjectOpenHashMap<>();
            for (var condition : cases().cases()) {
                var model = condition.getSecond().bake(baker);
                for (var when : condition.getFirst()) cases.put(when, model);
            }

            return new SelectUpgradeModel<>(cases().component(), cases, fallback().orElse(MISSING).bake(baker));
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            cases().cases().forEach(x -> x.getSecond().resolveDependencies(resolver));
            fallback().orElse(MISSING).resolveDependencies(resolver);
        }
    }

    private record Cases<T>(DataComponentType<T> component, List<Pair<List<T>, TurtleUpgradeModel.Unbaked>> cases) {
        private static final MapCodec<Cases<?>> CODEC = DataComponentType.CODEC.dispatchMap("property", Cases::component, Util.memoize(Cases::codec));

        private static <T> MapCodec<Cases<T>> codec(DataComponentType<T> component) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                MapCodec.unit(component).forGetter(Cases::component),
                caseCodec(component.codecOrThrow()).listOf().fieldOf("cases").validate(Cases::validate).forGetter(Cases::cases)
            ).apply(instance, Cases<T>::new));
        }

        private static <T> Codec<Pair<List<T>, TurtleUpgradeModel.Unbaked>> caseCodec(Codec<T> codec) {
            return RecordCodecBuilder.create(instance -> instance.group(
                codec.listOf().fieldOf("when").forGetter(Pair::getFirst),
                TurtleUpgradeModel.CODEC.fieldOf("model").forGetter(Pair::getSecond)
            ).apply(instance, Pair::new));
        }

        private static <T> DataResult<List<Pair<List<T>, TurtleUpgradeModel.Unbaked>>> validate(List<Pair<List<T>, TurtleUpgradeModel.Unbaked>> cases) {
            Multiset<T> multiset = HashMultiset.create();
            for (var condition : cases) multiset.addAll(condition.getFirst());

            if (multiset.isEmpty()) return DataResult.error(() -> "Empty cases");
            if (multiset.size() != multiset.entrySet().size()) {
                return DataResult.error(() -> "Duplicate case conditions: " + multiset.entrySet().stream()
                    .filter(x -> x.getCount() > 1)
                    .map(x -> Objects.toString(x.getElement()))
                    .collect(Collectors.joining(", ")));
            }

            return DataResult.success(cases);
        }
    }

    /**
     * Create a {@link SelectUpgradeModel} that selects a model based on a component.
     *
     * @param component The component to select.
     * @param <T>       The type the component stores.
     * @return A {@link Builder}.
     */
    public static <T> Builder<T> onComponent(DataComponentType<T> component) {
        return new Builder<>(component);
    }

    /**
     * A builder for constructing {@link SelectUpgradeModel}s.
     *
     * @param <T> The type of value to switch on.
     */
    public static final class Builder<T> {
        private final DataComponentType<T> component;
        private final List<Pair<List<T>, TurtleUpgradeModel.Unbaked>> cases = new ArrayList<>();
        private TurtleUpgradeModel.@Nullable Unbaked fallback;

        private Builder(DataComponentType<T> component) {
            this.component = component;
        }

        /**
         * Add a case to our model.
         *
         * @param value The value for this case.
         * @param model The model to use.
         * @return {@code this}, for chaining.
         */
        public Builder<T> when(T value, TurtleUpgradeModel.Unbaked model) {
            return when(List.of(value), model);
        }

        /**
         * Add a case to our model.
         *
         * @param values The value(s) for this case.
         * @param model  The model to use.
         * @return {@code this}, for chaining.
         */
        public Builder<T> when(List<T> values, TurtleUpgradeModel.Unbaked model) {
            cases.add(Pair.of(values, model));
            return this;
        }

        /**
         * Add a fallback value, when no previous value matches or the component is not present.
         *
         * @param model The fallback model.
         * @return {@code this}, for chaining.
         */
        public Builder<T> fallback(TurtleUpgradeModel.Unbaked model) {
            this.fallback = model;
            return this;
        }

        /**
         * Convert this builder into an unbaked model.
         *
         * @return The unbaked {@link SelectUpgradeModel}.
         */
        public TurtleUpgradeModel.Unbaked create() {
            return new Unbaked<>(new Cases<>(component, cases), Optional.ofNullable(fallback));
        }
    }
}
