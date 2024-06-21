package dan200.computercraft.api.client;

import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.impl.client.ClientPlatformHelper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * The location of a model to load. This may either be:
 *
 * <ul>
 *     <li>A {@link ModelResourceLocation}, referencing an already baked model (such as {@code minecraft:dirt#inventory}).</li>
 *     <li>
 *         A {@link ResourceLocation}, referencing a path to a model resource (such as {@code minecraft:item/dirt}.
 *         These models will be baked and stored in the {@link ModelManager} in a loader-specific way.
 *     </li>
 * </ul>
 */
public final class ModelLocation {
    /**
     * The location of the model.
     * <p>
     * When {@link #resourceLocation} is null, this is the location of the model to load. When {@link #resourceLocation}
     * is non-null, this is the "standalone" variant of the model resource — this is used by Forge's implementation of
     * {@link ClientPlatformHelper#getModel(ModelManager, ModelResourceLocation, ResourceLocation)} to fetch the model
     * from the model manger. It is not used on Fabric.
     */
    private final ModelResourceLocation modelLocation;
    private final @Nullable ResourceLocation resourceLocation;

    private ModelLocation(ModelResourceLocation modelLocation, @Nullable ResourceLocation resourceLocation) {
        this.modelLocation = modelLocation;
        this.resourceLocation = resourceLocation;
    }

    /**
     * Create a {@link ModelLocation} from model in the model manager.
     *
     * @param location The name of the model to load.
     * @return The new {@link ModelLocation} instance.
     */
    public static ModelLocation model(ModelResourceLocation location) {
        return new ModelLocation(location, null);
    }

    /**
     * Create a {@link ModelLocation} from a resource.
     *
     * @param location The location of the model resource, such as {@code minecraft:item/dirt}.
     * @return The new {@link ModelLocation} instance.
     */
    public static ModelLocation resource(ResourceLocation location) {
        return new ModelLocation(new ModelResourceLocation(location, "standalone"), location);
    }

    /**
     * Get this model from the provided model manager.
     *
     * @param manager The model manger.
     * @return This model, or the missing model if it could not be found.
     */
    public BakedModel getModel(ModelManager manager) {
        return ClientPlatformHelper.get().getModel(manager, modelLocation, resourceLocation);
    }

    /**
     * Get the models this model location depends on.
     *
     * @return A list of models that this model location depends on.
     * @see TurtleUpgradeModeller#getDependencies()
     */
    public Stream<ResourceLocation> getDependencies() {
        return resourceLocation == null ? Stream.empty() : Stream.of(resourceLocation);
    }
}
