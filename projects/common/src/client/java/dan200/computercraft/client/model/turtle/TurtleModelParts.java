// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.model.turtle;

import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.util.DataComponentUtil;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Combines several individual models together to form a turtle.
 *
 * @param <T> The type of the resulting "baked model".
 */
public final class TurtleModelParts<T> {
    private static final Transformation identity, flip;

    static {
        var stack = new PoseStack();
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.scale(1, -1, 1);
        stack.translate(-0.5f, -0.5f, -0.5f);

        identity = Transformation.identity();
        flip = new Transformation(stack.last().pose());
    }

    private record Combination(
        boolean colour,
        @Nullable UpgradeData<ITurtleUpgrade> leftUpgrade,
        @Nullable UpgradeData<ITurtleUpgrade> rightUpgrade,
        @Nullable TurtleOverlay overlay,
        boolean christmas,
        boolean flip
    ) {
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;
    private final Function<TransformedModel, BakedModel> transformer;
    private final Function<Combination, T> buildModel;

    /**
     * A cache of {@link TransformedModel} to the transformed {@link BakedModel}. This helps us pool the transformed
     * instances, reducing memory usage and hopefully ensuring their caches are hit more often!
     */
    private final Map<TransformedModel, BakedModel> transformCache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .<TransformedModel, BakedModel>build()
        .asMap();

    /**
     * A cache of {@link Combination}s to the combined model.
     */
    private final Map<Combination, T> modelCache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .<Combination, T>build()
        .asMap();

    public TurtleModelParts(BakedModel familyModel, BakedModel colourModel, ModelTransformer transformer, Function<List<BakedModel>, T> combineModel) {
        this.familyModel = familyModel;
        this.colourModel = colourModel;
        this.transformer = x -> transformer.transform(x.model(), x.matrix());
        buildModel = x -> combineModel.apply(buildModel(x));
    }

    public T getModel(ItemStack stack) {
        var combination = getCombination(stack);
        return modelCache.computeIfAbsent(combination, buildModel);
    }

    private Combination getCombination(ItemStack stack) {
        var christmas = Holiday.getCurrent() == Holiday.CHRISTMAS;
        var leftUpgrade = TurtleItem.getUpgradeWithData(stack, TurtleSide.LEFT);
        var rightUpgrade = TurtleItem.getUpgradeWithData(stack, TurtleSide.RIGHT);
        var overlay = TurtleItem.getOverlay(stack);
        var label = DataComponentUtil.getCustomName(stack);
        var flip = label != null && (label.equals("Dinnerbone") || label.equals("Grumm"));

        return new Combination(stack.has(DataComponents.DYED_COLOR), leftUpgrade, rightUpgrade, overlay, christmas, flip);
    }

    private List<BakedModel> buildModel(Combination combo) {
        var modelManager = Minecraft.getInstance().getModelManager();

        var transformation = combo.flip ? flip : identity;
        var parts = new ArrayList<BakedModel>(4);
        parts.add(transform(combo.colour() ? colourModel : familyModel, transformation));

        if (combo.overlay() != null) addPart(parts, modelManager, transformation, combo.overlay().model());

        var showChristmas = TurtleOverlay.showElfOverlay(combo.overlay(), combo.christmas());
        if (showChristmas) addPart(parts, modelManager, transformation, TurtleOverlay.ELF_MODEL);

        addUpgrade(parts, transformation, TurtleSide.LEFT, combo.leftUpgrade());
        addUpgrade(parts, transformation, TurtleSide.RIGHT, combo.rightUpgrade());

        return parts;
    }

    private void addPart(List<BakedModel> parts, ModelManager modelManager, Transformation transformation, ResourceLocation model) {
        parts.add(transform(ClientPlatformHelper.get().getModel(modelManager, model), transformation));
    }

    private void addUpgrade(List<BakedModel> parts, Transformation transformation, TurtleSide side, @Nullable UpgradeData<ITurtleUpgrade> upgrade) {
        if (upgrade == null) return;
        var model = TurtleUpgradeModellers.getModel(upgrade.upgrade(), upgrade.data(), side);
        parts.add(transform(model.model(), transformation.compose(model.matrix())));
    }

    private BakedModel transform(BakedModel model, Transformation transformation) {
        if (transformation.equals(Transformation.identity())) return model;
        return transformCache.computeIfAbsent(new TransformedModel(model, transformation), transformer);
    }

    public interface ModelTransformer {
        BakedModel transform(BakedModel model, Transformation transformation);
    }
}
