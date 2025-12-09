package com.example.examplemod;

import dan200.computercraft.api.detail.DetailRegistry;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * An example peripheral for pocket computers. This currently doesn't have an associated upgrade — it mostly exists to
 * demonstrate other functionality.
 */
public class ExamplePocketPeripheral implements IPeripheral {
    private final IPocketAccess pocket;

    public ExamplePocketPeripheral(IPocketAccess pocket) {
        this.pocket = pocket;
    }

    @Override
    public String getType() {
        return "example";
    }

    /**
     * An example of using {@linkplain DetailRegistry detail registries} to get the current player's held item.
     *
     * @return The item details, or {@code null} if the player is not holding an item.
     */
    // @start region=details
    @LuaFunction(mainThread = true)
    public final @Nullable Map<String, ?> getHeldItem() {
        if (!(pocket.getEntity() instanceof LivingEntity entity)) return null;

        var heldItem = entity.getItemInHand(InteractionHand.MAIN_HAND);
        return heldItem.isEmpty() ? null : VanillaDetailRegistries.ITEM_STACK.getDetails(pocket.getLevel().registryAccess(), heldItem);
    }
    // @end region=details

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ExamplePocketPeripheral o && pocket == o.pocket;
    }
}
