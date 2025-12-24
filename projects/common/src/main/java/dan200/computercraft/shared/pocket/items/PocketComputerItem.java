// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.UpgradeManager;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.*;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.computer.items.ServerComputerReference;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.pocket.core.PocketBrain;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.pocket.core.PocketSide;
import dan200.computercraft.shared.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PocketComputerItem extends Item {
    private final ComputerFamily family;

    public PocketComputerItem(Properties settings, ComputerFamily family) {
        super(settings);
        this.family = family;
    }

    /**
     * Tick a pocket computer.
     *
     * @param stack  The current pocket computer stack.
     * @param holder The entity holding the pocket item.
     * @param brain  The pocket brain.
     */
    public void tick(ItemStack stack, PocketHolder holder, PocketBrain brain) {
        brain.tick();

        // Sync pocket state back to the item
        if (updateItem(stack, brain)) holder.setChanged();
    }

    /**
     * Copy properties from the brain back to the item stack.
     *
     * @param stack The current pocket computer stack.
     * @param brain The current pocket brain.
     * @return Whether the item was changed.
     */
    private boolean updateItem(ItemStack stack, PocketBrain brain) {
        var changed = false;
        var computer = brain.computer();

        // Sync label
        var label = computer.getLabel();
        if (!Objects.equals(label, getLabel(stack))) {
            changed = true;
            DataComponentUtil.setCustomName(stack, label);
        }

        var on = computer.isOn();
        if (on != isMarkedOn(stack)) {
            changed = true;
            stack.set(ModRegistry.DataComponents.ON.get(), on);
        }

        return changed;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof ServerPlayer player) {
            var invSlot = InventoryUtil.findItemInInventory(player.getInventory(), stack);
            if (invSlot < 0) return;

            // If we're in the inventory, create a computer and keep it alive.
            var holder = new PocketHolder.PlayerHolder(player, invSlot);
            var brain = getOrCreateBrain(holder, stack);
            brain.computer().keepAlive();
            tick(stack, holder, brain);
        } else if (slot != null && entity instanceof LivingEntity living) {
            var holder = new PocketHolder.LivingEntityHolder(living, slot);
            var brain = getBrain(holder, stack);
            if (brain != null) tick(stack, holder, brain);
        }
    }

    @ForgeOverride
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        var level = entity.level();
        if (level.isClientSide() || level.getServer() == null) return false;

        // If we're an item entity, tick an already existing computer (as to update the position), but do not keep the
        // computer alive.
        var holder = new PocketHolder.ItemEntityHolder(entity);
        var brain = getBrain(holder, stack);
        if (brain != null) tick(stack, holder, brain);

        return false;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!world.isClientSide()) {
            var holder = new PocketHolder.PlayerHolder((ServerPlayer) player, InventoryUtil.getHandSlot(player, hand));
            var brain = getOrCreateBrain(holder, stack);
            var computer = brain.computer();
            computer.turnOn();

            var stop = brain.onRightClick((ServerLevel) world);
            if (!stop) openMenu(player, stack, holder, hand == InteractionHand.OFF_HAND, computer);
        }
        return InteractionResult.SUCCESS;
    }

    private static void openMenu(Player player, ItemStack stack, PocketHolder holder, boolean isTypingOnly, ServerComputer computer) {
        PlatformHelper.get().openMenu(player, stack.getHoverName(), (id, inventory, entity) -> new ComputerMenuWithoutInventory(
            isTypingOnly ? ModRegistry.Menus.POCKET_COMPUTER_NO_TERM.get() : ModRegistry.Menus.COMPUTER.get(), id, inventory,
            p -> holder.isValid(computer),
            computer
        ), new ComputerContainerData(computer, stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        return UpgradeManager.getName(getDescriptionId(), getUpgrade(stack, PocketSide.BACK), getUpgrade(stack, PocketSide.BOTTOM));
    }

    @Nullable
    @ForgeOverride
    public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {
        return PocketUpgrades.instance().getOwner(getUpgradeWithData(stack, PocketSide.BACK), getUpgradeWithData(stack, PocketSide.BOTTOM));
    }

    /**
     * Get (or create) the pocket brain and turn it on, ready for the player to interact with.
     *
     * @param stack  The pocket computer stack.
     * @param holder The holder of the pocket computer.
     * @return The pocket brain.
     */
    public PocketBrain getOrCreateBrain(PocketHolder holder, ItemStack stack) {
        var server = holder.level().getServer();
        var registry = ServerContext.get(server).registry();
        {
            var computer = getServerComputer(registry, stack);
            if (computer != null) {
                var brain = computer.getBrain();
                brain.updateHolder(holder);
                return brain;
            }
        }

        var computerID = NonNegativeId.getOrCreate(server, stack, ModRegistry.DataComponents.COMPUTER_ID.get(), NonNegativeId.Computer::new, IDAssigner.COMPUTER);
        var brain = new PocketBrain(holder, ServerComputer.properties(computerID, getFamily())
            .label(getLabel(stack))
            .storageCapacity(StorageCapacity.getOrDefault(stack.get(ModRegistry.DataComponents.STORAGE_CAPACITY.get()), -1))
            .terminalSize(stack.getOrDefault(
                ModRegistry.DataComponents.TERMINAL_SIZE.get(),
                new TerminalSize(ConfigSpec.pocketTermWidth.get(), ConfigSpec.pocketTermHeight.get())
            ))
        );
        brain.setUpgrades(getUpgradeWithData(stack, PocketSide.BACK), getUpgradeWithData(stack, PocketSide.BOTTOM));
        var computer = brain.computer();

        stack.set(ModRegistry.DataComponents.COMPUTER.get(), new ServerComputerReference(registry.getSessionID(), computer.register()));

        if (isMarkedOn(stack)) computer.turnOn();

        updateItem(stack, brain);

        holder.setChanged();

        return brain;
    }

    public @Nullable PocketBrain getBrain(PocketHolder holder, ItemStack stack) {
        var computer = getServerComputer(holder.level().getServer(), stack);
        if (computer == null) return null;

        var brain = computer.getBrain();
        brain.updateHolder(holder);
        return brain;
    }

    public static boolean isServerComputer(ServerComputer computer, ItemStack stack) {
        return stack.getItem() instanceof PocketComputerItem
            && getServerComputer(computer.getLevel().getServer(), stack) == computer;
    }

    private static @Nullable PocketServerComputer getServerComputer(ServerComputerRegistry registry, ItemStack stack) {
        return (PocketServerComputer) ServerComputerReference.get(stack, registry);
    }

    private static @Nullable PocketServerComputer getServerComputer(MinecraftServer server, ItemStack stack) {
        return getServerComputer(ServerContext.get(server).registry(), stack);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        // Normally we treat the computer instance as the source of truth, and copy the computer's state back to the
        // item. However, if we've just crafted the computer with an upgrade, we should sync the other way, and update
        // the computer.
        var server = level.getServer();
        if (server == null) return;

        var computer = getServerComputer(server, stack);
        if (computer == null) return;

        computer.getBrain().setUpgrades(getUpgradeWithData(stack, PocketSide.BACK), getUpgradeWithData(stack, PocketSide.BOTTOM));
    }

    public ComputerFamily getFamily() {
        return family;
    }

    // IMedia

    private @Nullable String getLabel(ItemStack stack) {
        return DataComponentUtil.getCustomName(stack);
    }

    private static boolean isMarkedOn(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.DataComponents.ON.get(), false);
    }

    public static @Nullable IPocketUpgrade getUpgrade(ItemStack stack, PocketSide side) {
        var upgrade = getUpgradeWithData(stack, side);
        return upgrade == null ? null : upgrade.upgrade();
    }

    public static @Nullable UpgradeData<IPocketUpgrade> getUpgradeWithData(ItemStack stack, PocketSide side) {
        return stack.get(switch (side) {
            case BACK -> ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get();
            case BOTTOM -> ModRegistry.DataComponents.BOTTOM_POCKET_UPGRADE.get();
        });
    }
}
