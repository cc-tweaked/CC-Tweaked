package dan200.computercraft.testmod;

import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.UpgradeBase;

import java.util.function.Supplier;

public class ModEntrypoint {
    public static final String ENCHANTABLE_TOOL = "enchantable_tool";
    public static Supplier<TurtleUpgradeSerialiser<EnchantableTurtleTool>> buildEnchantableTurtleTool() {
        return () -> TurtleUpgradeSerialiser.simpleWithCustomItem(
            (id, itemStack) -> new EnchantableTurtleTool(
                id, UpgradeBase.getDefaultAdjective(id), itemStack.getItem(), itemStack, 3.0f, null
            )
        );
    }
}
