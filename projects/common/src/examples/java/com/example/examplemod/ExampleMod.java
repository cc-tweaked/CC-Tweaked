package com.example.examplemod;

import com.example.examplemod.data.TurtleUpgradeProvider;
import com.example.examplemod.peripheral.FurnacePeripheral;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeType;

/**
 * Our example mod, containing the various things we register.
 * <p>
 * This isn't an especially good template to follow! It's convenient for our example mod (as we need to be multi-loader
 * compatible), but there's a good chance there's a better pattern to follow. For example, on Forge you'd use
 * {@code DeferredRegister} to register things), and multi-loader mods probably have their own abstractions.
 * <p>
 * See {@code FabricExampleMod} and {@code ForgeExampleMod} for the actual mod entrypoints.
 */
public final class ExampleMod {
    public static final String MOD_ID = "examplemod";

    /**
     * The upgrade type for our example turtle upgrade. See the documentation for {@link UpgradeType} or
     * {@code FabricExampleMod}/{@code ForgeExampleMod} for how this is registered.
     * <p>
     * This only defines the upgrade type. See {@link TurtleUpgradeProvider} for defining the actual upgrade.
     */
    // @start region=turtle_upgrades
    public static final UpgradeType<ExampleTurtleUpgrade> EXAMPLE_TURTLE_UPGRADE = UpgradeType.simpleWithCustomItem(
        ExampleTurtleUpgrade::new
    );
    // @end region=turtle_upgrades

    public static void registerComputerCraft() {
        // @start region=generic_source
        ComputerCraftAPI.registerGenericSource(new FurnacePeripheral());
        // @end region=generic_source

        // @start region=details
        VanillaDetailRegistries.ITEM_STACK.addProvider((out, stack) -> {
            var food = stack.getItem().getFoodProperties();
            if (food == null) return;

            out.put("saturation", food.getSaturationModifier());
            out.put("nutrition", food.getNutrition());
        });
        // @end region=details

        ExampleAPI.register();
    }
}
