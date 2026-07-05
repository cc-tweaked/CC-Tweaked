// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.jei;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.RecipeModHelpers;
import dan200.computercraft.shared.pocket.core.PocketSide;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.List;

@JeiPlugin
public class JEIComputerCraft implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.TURTLE_NORMAL.get(), turtleSubtype);
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.TURTLE_ADVANCED.get(), turtleSubtype);

        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), pocketSubtype);
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get(), pocketSubtype);

        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModRegistry.Items.DISK.get(), diskSubtype);
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registry) {
        registry.addSimpleRecipeManagerPlugin(RecipeTypes.CRAFTING, new RecipeResolver(getRegistryAccess()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var registry = runtime.getRecipeManager();

        // Register all turtles/pocket computers (not just vanilla upgrades) as upgrades on JEI.
        var upgradeItems = RecipeModHelpers.getExtraStacks(getRegistryAccess());
        if (!upgradeItems.isEmpty()) {
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, upgradeItems);
        }

        // Hide all upgrade recipes
        var category = registry.createRecipeLookup(RecipeTypes.CRAFTING);
        category.get().forEach(wrapper -> {
            if (RecipeModHelpers.shouldRemoveRecipe(wrapper.id().identifier())) {
                registry.hideRecipes(RecipeTypes.CRAFTING, List.of(wrapper));
            }
        });
    }

    /**
     * Distinguishes turtles by upgrades and family.
     */
    private static final ISubtypeInterpreter<ItemStack> turtleSubtype = (stack, ctx) -> {
        var name = new StringBuilder("turtle:");

        // Add left and right upgrades to the identifier
        var left = TurtleItem.getUpgradeWithData(stack, TurtleSide.LEFT);
        var right = TurtleItem.getUpgradeWithData(stack, TurtleSide.RIGHT);
        if (left != null) name.append(left.holder().key().identifier());
        if (left != null && right != null) name.append('|');
        if (right != null) name.append(right.holder().key().identifier());

        return name.toString();
    };

    /**
     * Distinguishes pocket computers by upgrade and family.
     */
    private static final ISubtypeInterpreter<ItemStack> pocketSubtype = (stack, ctx) -> {
        var name = new StringBuilder("pocket:");

        // Add the upgrade to the identifier
        var top = PocketComputerItem.getUpgradeWithData(stack, PocketSide.TOP);
        var back = PocketComputerItem.getUpgradeWithData(stack, PocketSide.BACK);
        if (top != null) name.append(top.holder().key().identifier());
        if (top != null && back != null) name.append('|');
        if (back != null) name.append(back.holder().key().identifier());

        return name.toString();
    };

    /**
     * Distinguishes disks by colour.
     */
    private static final ISubtypeInterpreter<ItemStack> diskSubtype = (stack, ctx) -> Integer.toString(DyedItemColor.getOrDefault(stack, -1));

    private static HolderLookup.Provider getRegistryAccess() {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? RegistryAccess.EMPTY : connection.registryAccess();
    }
}
