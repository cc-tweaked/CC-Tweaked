// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.client.turtle.RegisterTurtleUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.item.colour.PocketComputerLight;
import dan200.computercraft.client.item.model.TurtleOverlayModel;
import dan200.computercraft.client.item.properties.PocketComputerStateProperty;
import dan200.computercraft.client.item.properties.TurtleShowElfOverlay;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.client.render.TurtleBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.turtle.TurtleModemModel;
import dan200.computercraft.client.turtle.TurtleUpgradeModels;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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
        return models.computeIfAbsent(model, m -> ClientPlatformHelper.get().createModelKey(m, m::toString));
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
        register.register(ModRegistry.TurtleUpgradeTypes.SPEAKER.get(), TurtleUpgradeModel.sided(
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_left"),
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_speaker_right")
        ));
        register.register(ModRegistry.TurtleUpgradeTypes.WORKBENCH.get(), TurtleUpgradeModel.sided(
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_left"),
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_crafting_table_right")
        ));
        register.register(ModRegistry.TurtleUpgradeTypes.WIRELESS_MODEM.get(), TurtleModemModel.UNBAKED);
        register.register(ModRegistry.TurtleUpgradeTypes.TOOL.get(), TurtleUpgradeModel.flatItem());
    }

    public static void registerReloadListeners(BiConsumer<ResourceLocation, PreparableReloadListener> register, Minecraft minecraft) {
        register.accept(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "sprites"), GuiSprites.initialise(minecraft.getTextureManager()));
    }

    private static final ResourceLocation[] EXTRA_MODELS = {
        TurtleOverlay.ELF_MODEL,
        TurtleBlockEntityRenderer.NORMAL_TURTLE_MODEL,
        TurtleBlockEntityRenderer.ADVANCED_TURTLE_MODEL,
        TurtleBlockEntityRenderer.COLOUR_TURTLE_MODEL,
        MissingBlockModel.LOCATION,
    };

    public static void registerExtraModels(
        BiConsumer<ModelKey<StandaloneModel>, ResourceLocation> registerBasic,
        BiConsumer<ModelKey<? extends TurtleUpgradeModel<?>>, TurtleUpgradeModel.Unbaked<?>> registerTurtle,
        Collection<ResourceLocation> extraModels
    ) {
        for (var model : EXTRA_MODELS) registerBasic.accept(getModel(model), model);
        for (var model : extraModels) registerBasic.accept(getModel(model), model);
        TurtleUpgradeModels.bake(registerTurtle);
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
}
