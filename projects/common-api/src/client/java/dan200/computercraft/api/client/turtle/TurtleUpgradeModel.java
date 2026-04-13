// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.client.ComputerCraftAPIClientService;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

/**
 * The model for a {@link ITurtleUpgrade}.
 * <p>
 * Turtle upgrade models are very similar to vanilla's {@link ItemModel}. Each upgrade's model is defined in JSON, and
 * loaded from resource packs with other assets.
 * <p>
 * In most cases, upgrades can use one of the existing implementations of {@link TurtleUpgradeModel} (e.g.
 * {@link BasicUpgradeModel} or {@link ItemUpgradeModel}), and do not need to subclass it. However, in the cases where
 * a custom model is required, one should use
 * {@code dan200.computercraft.api.client.FabricComputerCraftAPIClient#registerTurtleUpgradeModel} to register a
 * model on Fabric and {@code dan200.computercraft.api.client.turtle.RegisterTurtleModelEvent} to register one
 * on Forge.
 * <p>
 * See {@link ITurtleUpgrade} for a full example of registering turtle upgrades and their models.
 *
 * @see RegisterTurtleUpgradeModel For multi-loader registration support.
 * @see ItemUpgradeModel A {@code TurtleUpgradeModel} which uses the upgrade's item.
 * @see BasicUpgradeModel A {@code TurtleUpgradeModel} which renders a simple model.
 */
public interface TurtleUpgradeModel {
    /**
     * The directory from which turtle upgrade models are loaded. This may be used by data generators.
     */
    String SOURCE = ComputerCraftAPI.MOD_ID + "/turtle_upgrade";

    /**
     * The codec used to read/write {@linkplain TurtleUpgradeModel.Unbaked unbaked upgrade models}.
     */
    Codec<TurtleUpgradeModel.Unbaked> CODEC = Codec.lazyInitialized(() -> ComputerCraftAPIClientService.get().getTurtleUpgradeModelCodec());

    /**
     * Render this upgrade to an {@link ItemStackRenderState}. This is used for rendering the item form of the upgrade.
     * <p>
     * Like with {@link ItemModel}, implementations must be careful to call {@link ItemStackRenderState#appendModelIdentityElement}
     * where appropriate.
     *
     * @param upgrade   The upgrade being rendered.
     * @param side      Which side of the turtle (left or right) the upgrade is equipped on.
     * @param renderer  The render state to draw to.
     * @param resolver  The model resolver.
     * @param transform The root model's transformation.
     * @param seed      The current model seed.
     * @see ItemModel#update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, ItemOwner, int)
     */
    void renderForItem(UpgradeData<ITurtleUpgrade> upgrade, TurtleSide side, ItemStackRenderState renderer, ItemModelResolver resolver, ItemTransform transform, int seed);

    /**
     * An unbaked turtle model. Much like other unbaked models (e.g. {@link ItemModel.Unbaked}), this should resolve
     * any dependencies and returned the fully-resolved model.
     */
    interface Unbaked extends ResolvableModel {
        /**
         * The {@link MapCodec} used to read/write this unbaked model.
         *
         * @return The codec used to read/write this model.
         * @see ItemModel.Unbaked#type()
         */
        MapCodec<? extends Unbaked> type();

        /**
         * Bake this turtle model.
         *
         * @param baker The current model baker
         * @return The baked upgrade model.
         * @see ItemModel.Unbaked#bake(ItemModel.BakingContext, Matrix4fc)
         */
        TurtleUpgradeModel bake(ModelBaker baker);
    }
}
