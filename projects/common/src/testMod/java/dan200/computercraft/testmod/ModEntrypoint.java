package dan200.computercraft.testmod;

import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RegistrationHelper;
import dan200.computercraft.shared.platform.RegistryEntry;

import javax.annotation.Nullable;

public class ModEntrypoint {
    public static @Nullable RegistryEntry<TurtleUpgradeSerialiser<EnchantableTurtleTool>> enchantableTurtleTool = null;
    public static void init() {
        RegistrationHelper<TurtleUpgradeSerialiser<?>> turtleSerializerRegistry = PlatformHelper.get().createRegistrationHelper(TurtleUpgradeSerialiser.registryId());
        enchantableTurtleTool = turtleSerializerRegistry.register(
            "enchantable_tool",
            () -> TurtleUpgradeSerialiser.simpleWithCustomItem(
                (id, itemStack) -> new EnchantableTurtleTool(
                    id, UpgradeBase.getDefaultAdjective(id), itemStack.getItem(), itemStack, 3.0f, null
                )
            )
        );
        System.out.println(enchantableTurtleTool);
    }
}
