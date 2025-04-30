// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The model for a {@link ITurtleUpgrade}.
 * <p>
 * Use {@code dan200.computercraft.api.client.FabricComputerCraftAPIClient#registerTurtleUpgradeModeller} to register a
 * modeller on Fabric and {@code dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent} to register one
 * on Forge.
 *
 * <h2>Example</h2>
 * <h3>Fabric</h3>
 * {@snippet class=com.example.examplemod.FabricExampleModClient region=turtle_model}
 *
 * <h3>Forge</h3>
 * {@snippet class=com.example.examplemod.FabricExampleModClient region=turtle_model}
 *
 * @param <T> The type of turtle upgrade this modeller applies to.
 * @see RegisterTurtleUpgradeModel For multi-loader registration support.
 */
public interface TurtleUpgradeModel<T extends ITurtleUpgrade> {
    /**
     * Render this upgrade to an {@link ItemStackRenderState}. This is used for rendering the item form of the upgrade.
     *
     * @param upgrade   The upgrade being rendered.
     * @param side      Which side of the turtle (left or right) the upgrade resides on.
     * @param data      Upgrade data instance for current turtle side.
     * @param renderer  The render state to draw to.
     * @param resolver  The model resolver.
     * @param transform The root model's transformation.
     * @param seed      The current model seed.
     * @see ItemModel#update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, LivingEntity, int)
     */
    void renderForItem(T upgrade, TurtleSide side, DataComponentPatch data, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed);

    /**
     * Render this upgrade to a {@link MultiBufferSource}. This is used for rendering the block-entity form of the
     * upgrade.
     *
     * @param upgrade   The upgrade being rendered.
     * @param turtle    Access to the turtle that the upgrade resides on. This will be null when getting item models.
     * @param side      Which side of the turtle (left or right) the upgrade resides on.
     * @param data      Upgrade data instance for current turtle side.
     * @param transform The current pose stack.
     * @param buffers   The buffers to render to.
     * @param light     The lightmap coordinate.
     * @param overlay   The overlay coordinate.
     */
    void renderForLevel(T upgrade, ITurtleAccess turtle, TurtleSide side, DataComponentPatch data, PoseStack transform, MultiBufferSource buffers, int light, int overlay);

    /**
     * An unbaked turtle model. Much like other unbaked models (e.g. {@link ItemModel.Unbaked}), this should resolve
     * any dependencies and returned the fully-resolved model.
     *
     * @param <T> The type of turtle upgrade for this model.
     */
    interface Unbaked<T extends ITurtleUpgrade> extends ResolvableModel {
        TurtleUpgradeModel<T> bake(ModelBaker baker);
    }

    /**
     * A basic {@link TurtleUpgradeModel} which renders using the upgrade's {@linkplain ITurtleUpgrade#getUpgradeItem(DataComponentPatch)
     * upgrade item}.
     * <p>
     * This uses appropriate transformations for "flat" items, namely those extending the {@literal minecraft:item/generated}
     * model type. It will not appear correct for 3D models with additional depth, such as blocks.
     *
     * @param <T> The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeModel.Unbaked<? super T> flatItem() {
        return ItemUpgradeModel.UNBAKED;
    }

    /**
     * Construct a {@link TurtleUpgradeModel} which has a single model for the left and right side.
     *
     * @param left  The model to use on the left.
     * @param right The model to use on the right.
     * @param <T>   The type of the turtle upgrade.
     * @return The constructed modeller.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeModel.Unbaked<T> sided(ResourceLocation left, ResourceLocation right) {
        return new SidedUpgradeModel.Unbaked<>(left, right);
    }
}
