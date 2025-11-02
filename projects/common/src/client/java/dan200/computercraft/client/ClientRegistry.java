// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.client.turtle.*;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.item.colour.PocketComputerLight;
import dan200.computercraft.client.item.model.TurtleOverlayModel;
import dan200.computercraft.client.item.properties.PocketComputerStateProperty;
import dan200.computercraft.client.item.properties.TurtleShowElfOverlay;
import dan200.computercraft.client.model.LecternBookModel;
import dan200.computercraft.client.model.LecternPocketModel;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.turtle.TurtleOverlay;
import dan200.computercraft.client.turtle.TurtleOverlayManager;
import dan200.computercraft.client.turtle.TurtleUpgradeModelManager;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers client-side objects, such as {@link BlockEntityRendererProvider}s and
 * {@link MenuScreens.ScreenConstructor}.
 * <p>
 * The functions in this class should be called from a loader-specific class.
 *
 * @see ModRegistry The common registry for actual game objects.
 */
public final class ClientRegistry {
    private ClientRegistry() {
    }

    private static final Map<ResourceLocation, ModelKey<StandaloneModel>> models = new ConcurrentHashMap<>();

    public static ModelKey<StandaloneModel> getModel(ResourceLocation model) {
        return models.computeIfAbsent(model, m -> ClientPlatformHelper.get().createModelKey(m::toString));
    }

    public static StandaloneModel getModel(ModelManager manager, ResourceLocation modelId) {
        var model = getModel(modelId).get(manager);
        if (model != null) return model;

        return Objects.requireNonNull(getModel(MissingBlockModel.LOCATION).get(manager));
    }

    /**
     * Register any client-side objects which don't have to be done on the main thread.
     */
    public static void register() {
        BlockEntityRenderers.register(ModRegistry.BlockEntities.MONITOR_NORMAL.get(), MonitorBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.MONITOR_ADVANCED.get(), MonitorBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.TURTLE_NORMAL.get(), TurtleBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.TURTLE_ADVANCED.get(), TurtleBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModRegistry.BlockEntities.LECTERN.get(), CustomLecternRenderer::new);
    }

    public static void registerMenuScreens(RegisterMenuScreen register) {
        register.<AbstractComputerMenu, ComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.COMPUTER.get(), ComputerScreen::new);
        register.<AbstractComputerMenu, NoTermComputerScreen<AbstractComputerMenu>>register(ModRegistry.Menus.POCKET_COMPUTER_NO_TERM.get(), NoTermComputerScreen::new);
        register.register(ModRegistry.Menus.TURTLE.get(), TurtleScreen::new);

        register.register(ModRegistry.Menus.PRINTER.get(), PrinterScreen::new);
        register.register(ModRegistry.Menus.DISK_DRIVE.get(), DiskDriveScreen::new);
        register.register(ModRegistry.Menus.PRINTOUT.get(), PrintoutScreen::new);
    }

    public interface RegisterMenuScreen {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory);
    }

    public static void registerTurtleModels(RegisterTurtleUpgradeModel register) {
        register.register(BasicUpgradeModel.ID, BasicUpgradeModel.CODEC);
        register.register(ItemUpgradeModel.ID, ItemUpgradeModel.CODEC);
        register.register(SelectUpgradeModel.ID, SelectUpgradeModel.CODEC);
    }

    private static final ResourceLocation[] EXTRA_MODELS = {
        TurtleOverlay.ELF_MODEL,
        TurtleBlockEntityRenderer.NORMAL_TURTLE_MODEL,
        TurtleBlockEntityRenderer.ADVANCED_TURTLE_MODEL,
        TurtleBlockEntityRenderer.COLOUR_TURTLE_MODEL,
        MissingBlockModel.LOCATION,
    };

    /**
     * Additional models to load.
     *
     * @param turtleOverlays The unbaked turtle models.
     * @param turtleUpgrades The unbaked turtle upgrades.
     * @see #gatherExtraModels(ResourceManager, Executor)
     * @see #registerExtraModels(RegisterExtraModels, ExtraModels)
     */
    public record ExtraModels(
        Map<ResourceLocation, TurtleOverlay.Unbaked> turtleOverlays,
        Map<ResourceLocation, TurtleUpgradeModel.Unbaked> turtleUpgrades
    ) {
    }

    /**
     * Gather the list of extra models to load.
     *
     * @param resources The current resource manager.
     * @param executor  The executor to schedule loading on.
     * @return A promise which contains our extra models.
     */
    public static CompletableFuture<ExtraModels> gatherExtraModels(ResourceManager resources, Executor executor) {
        var turtleOverlays = TurtleOverlayManager.loader().load(resources, executor);
        var turtleUpgrades = TurtleUpgradeModelManager.loader().load(resources, executor);
        return turtleOverlays.thenCombine(turtleUpgrades, ExtraModels::new);
    }

    /**
     * A callback used to register a model for a {@link ModelKey}.
     */
    public interface RegisterExtraModels {
        default <U extends ResolvableModel, T> void register(ModelKey<T> key, U unbaked, BiFunction<U, ModelBaker, T> bake) {
            register(key, unbaked, ResolvableModel::resolveDependencies, bake);
        }

        /**
         * Register an extra model.
         * <p>
         * This accepts functions to resolve dependencies and bake the model. While this would be conceptually nicer as
         * an interface, it would require multiple adaptors to convert between "upgrade model", "a"bstract model" and
         * "platform-specific model", so working with functions is cleaner.
         *
         * @param key     The model key for this model.
         * @param unbaked The unbaked model.
         * @param resolve The function to resolve dependencies for this model.
         * @param bake    The function to bake this model.
         * @param <U>     The type of unbaked model.
         * @param <T>     The type of baked model.
         */
        <U, T> void register(ModelKey<T> key, U unbaked, BiConsumer<U, ResolvableModel.Resolver> resolve, BiFunction<U, ModelBaker, T> bake);
    }

    public static void registerExtraModels(RegisterExtraModels register, ExtraModels models) {
        for (var model : EXTRA_MODELS) {
            register.register(getModel(model), model, (id, r) -> r.markDependency(id), StandaloneModel::of);
        }
        TurtleOverlayManager.loader().register(register, models.turtleOverlays());
        TurtleUpgradeModelManager.loader().register(register, models.turtleUpgrades());
    }

    public static void registerItemModels(BiConsumer<ResourceLocation, MapCodec<? extends ItemModel.Unbaked>> register) {
        register.accept(TurtleOverlayModel.ID, TurtleOverlayModel.CODEC);
        register.accept(dan200.computercraft.client.item.model.TurtleUpgradeModel.ID, dan200.computercraft.client.item.model.TurtleUpgradeModel.CODEC);
    }

    public static void registerItemColours(BiConsumer<ResourceLocation, MapCodec<? extends ItemTintSource>> register) {
        register.accept(PocketComputerLight.ID, PocketComputerLight.CODEC);
    }

    public static void registerSelectItemProperties(BiConsumer<ResourceLocation, SelectItemModelProperty.Type<?, ?>> register) {
        register.accept(PocketComputerStateProperty.ID, PocketComputerStateProperty.TYPE);
    }

    public static void registerConditionalItemProperties(BiConsumer<ResourceLocation, MapCodec<? extends ConditionalItemModelProperty>> register) {
        register.accept(TurtleShowElfOverlay.ID, TurtleShowElfOverlay.CODEC);
    }

    public static void registerLayerDefinitions(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> register) {
        register.accept(LecternBookModel.LAYER, LecternBookModel::createLayer);
        register.accept(LecternPrintoutModel.LAYER, LecternPrintoutModel::createLayer);
        register.accept(LecternPocketModel.LAYER, LecternPocketModel::createLayer);
    }

    public interface RegisterPictureInPictureRenderer {
        <T extends PictureInPictureRenderState> void register(Class<T> state, Function<MultiBufferSource.BufferSource, PictureInPictureRenderer<T>> factory);
    }

    public static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderer register) {
        register.register(PrintoutScreen.PrintoutRenderState.class, PrintoutScreen.PrintoutPictureRenderer::new);
    }

    public static void registerDebugScreenEntries(BiConsumer<ResourceLocation, DebugScreenEntry> register) {
        register.accept(LookingAtBlockEntityDebugEntry.ID, LookingAtBlockEntityDebugEntry.create());
    }
}
