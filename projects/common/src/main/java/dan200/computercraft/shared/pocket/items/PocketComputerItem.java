// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.pocket.items;

import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.lectern.CustomLecternBlock;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.pocket.core.PocketBrain;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PocketComputerItem extends Item implements IComputerItem, IMedia, IColouredItem {
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    public static final String NBT_ON = "On";

    private static final String NBT_INSTANCE = "InstanceId";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;

    public PocketComputerItem(Properties settings, ComputerFamily family) {
        super(settings);
        this.family = family;
    }

    public ItemStack create(int id, @Nullable String label, int colour, @Nullable UpgradeData<IPocketUpgrade> upgrade) {
        var result = new ItemStack(this);
        if (id >= 0) result.getOrCreateTag().putInt(NBT_ID, id);
        if (label != null) result.setHoverName(Component.literal(label));
        if (upgrade != null) {
            result.getOrCreateTag().putString(NBT_UPGRADE, upgrade.upgrade().getUpgradeID().toString());
            if (!upgrade.data().isEmpty()) result.getOrCreateTag().put(NBT_UPGRADE_INFO, upgrade.data().copy());
        }
        if (colour != -1) result.getOrCreateTag().putInt(NBT_COLOUR, colour);
        return result;
    }

    /**
     * Tick a pocket computer.
     *
     * @param stack   The current pocket computer stack.
     * @param holder  The entity holding the pocket item.
     * @param passive If set, the pocket computer will not be created if it doesn't exist, and will not be kept alive.
     */
    public void tick(ItemStack stack, PocketHolder holder, boolean passive) {
        PocketBrain brain;
        if (passive) {
            var computer = getServerComputer(holder.level().getServer(), stack);
            if (computer == null) return;
            brain = computer.getBrain();
        } else {
            brain = getOrCreateBrain(holder.level(), holder, stack);
            brain.computer().keepAlive();
        }

        // Update pocket upgrade
        var upgrade = brain.getUpgrade();
        if (upgrade != null) upgrade.upgrade().update(brain, brain.computer().getPeripheral(ComputerSide.BACK));

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
        var changed = brain.updateItem(stack);
        var computer = brain.computer();

        // Sync ID
        var id = computer.getID();
        if (id != getComputerID(stack)) {
            changed = true;
            setComputerID(stack, id);
        }

        // Sync label
        var label = computer.getLabel();
        if (!Objects.equals(label, getLabel(stack))) {
            changed = true;
            setLabel(stack, label);
        }

        var on = computer.isOn();
        if (on != isMarkedOn(stack)) {
            changed = true;
            stack.getOrCreateTag().putBoolean(NBT_ON, on);
        }

        return changed;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int compartmentSlot, boolean selected) {
        // This (in vanilla at least) is only called for players. Don't bother to handle other entities.
        if (world.isClientSide || !(entity instanceof ServerPlayer player)) return;

        // Find the actual slot the item exists in, aborting if it can't be found.
        var slot = InventoryUtil.getInventorySlotFromCompartment(player, compartmentSlot, stack);
        if (slot < 0) return;

        // If we're in the inventory, create a computer and keep it alive.
        tick(stack, new PocketHolder.PlayerHolder(player, slot), false);
    }

    @ForgeOverride
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        var level = entity.level();
        if (level.isClientSide || level.getServer() == null) return false;

        // If we're an item entity, tick an already existing computer (as to update the position), but do not keep the
        // computer alive.
        tick(stack, new PocketHolder.ItemEntityHolder(entity), true);

        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return CustomLecternBlock.defaultUseItemOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            var holder = new PocketHolder.PlayerHolder((ServerPlayer) player, InventoryUtil.getHandSlot(player, hand));
            var brain = getOrCreateBrain((ServerLevel) world, holder, stack);
            var computer = brain.computer();
            computer.turnOn();

            var stop = false;
            var upgrade = getUpgrade(stack);
            if (upgrade != null) {
                stop = upgrade.onRightClick(world, brain, computer.getPeripheral(ComputerSide.BACK));
                // Sync back just in case. We don't need to setChanged, as we'll return the item anyway.
                updateItem(stack, brain);
            }

            if (!stop) openImpl(player, stack, holder, hand == InteractionHand.OFF_HAND, computer);
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    /**
     * Open a container for this pocket computer.
     *
     * @param player       The player to show the menu for.
     * @param stack        The pocket computer stack.
     * @param holder       The holder of the pocket computer.
     * @param isTypingOnly Open the off-hand pocket screen (only supporting typing, with no visible terminal).
     */
    public void open(Player player, ItemStack stack, PocketHolder holder, boolean isTypingOnly) {
        var brain = getOrCreateBrain(holder.level(), holder, stack);
        var computer = brain.computer();
        computer.turnOn();
        openImpl(player, stack, holder, isTypingOnly, computer);
    }

    private static void openImpl(Player player, ItemStack stack, PocketHolder holder, boolean isTypingOnly, ServerComputer computer) {
        PlatformHelper.get().openMenu(player, stack.getHoverName(), (id, inventory, entity) -> new ComputerMenuWithoutInventory(
            isTypingOnly ? ModRegistry.Menus.POCKET_COMPUTER_NO_TERM.get() : ModRegistry.Menus.COMPUTER.get(), id, inventory,
            p -> holder.isValid(computer),
            computer
        ), new ComputerContainerData(computer, stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        var baseString = getDescriptionId(stack);
        var upgrade = getUpgrade(stack);
        if (upgrade != null) {
            return Component.translatable(baseString + ".upgraded",
                Component.translatable(upgrade.getUnlocalisedAdjective())
            );
        } else {
            return super.getName(stack);
        }
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        if (flag.isAdvanced() || getLabel(stack) == null) {
            var id = getComputerID(stack);
            if (id >= 0) {
                list.add(Component.translatable("gui.computercraft.tooltip.computer_id", id)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Nullable
    @ForgeOverride
    public String getCreatorModId(ItemStack stack) {
        var upgrade = getUpgrade(stack);
        if (upgrade != null) {
            // If we're a non-vanilla, non-CC upgrade then return whichever mod this upgrade
            // belongs to.
            var mod = PocketUpgrades.instance().getOwner(upgrade);
            if (mod != null && !mod.equals(ComputerCraftAPI.MOD_ID)) return mod;
        }

        return ComputerCraftAPI.MOD_ID;
    }

    private PocketBrain getOrCreateBrain(ServerLevel level, PocketHolder holder, ItemStack stack) {
        var registry = ServerContext.get(level.getServer()).registry();
        {
            var computer = getServerComputer(registry, stack);
            if (computer != null) {
                var brain = computer.getBrain();
                brain.updateHolder(holder);
                return brain;
            }
        }

        var computerID = getComputerID(stack);
        if (computerID < 0) {
            computerID = ComputerCraftAPI.createUniqueNumberedSaveDir(level.getServer(), IDAssigner.COMPUTER);
            setComputerID(stack, computerID);
        }

        var brain = new PocketBrain(
            holder, getUpgradeWithData(stack),
            ServerComputer.properties(getComputerID(stack), getFamily()).label(getLabel(stack))
        );
        var computer = brain.computer();

        var tag = stack.getOrCreateTag();
        tag.putInt(NBT_SESSION, registry.getSessionID());
        tag.putUUID(NBT_INSTANCE, computer.register());

        if (isMarkedOn(stack)) computer.turnOn();

        updateItem(stack, brain);

        holder.setChanged();

        return brain;
    }

    public static boolean isServerComputer(ServerComputer computer, ItemStack stack) {
        return stack.getItem() instanceof PocketComputerItem
            && getServerComputer(computer.getLevel().getServer(), stack) == computer;
    }

    @Nullable
    public static PocketServerComputer getServerComputer(ServerComputerRegistry registry, ItemStack stack) {
        return (PocketServerComputer) registry.get(getSessionID(stack), getInstanceID(stack));
    }

    @Nullable
    public static PocketServerComputer getServerComputer(MinecraftServer server, ItemStack stack) {
        return getServerComputer(ServerContext.get(server).registry(), stack);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        var tag = stack.getTag();
        if (tag == null) return;

        // Normally we treat the computer instance as the source of truth, and copy the computer's state back to the
        // item. However, if we've just crafted the computer with an upgrade, we should sync the other way, and update
        // the computer.
        var server = level.getServer();
        if (server != null) {
            var computer = getServerComputer(server, stack);
            if (computer != null) computer.getBrain().setUpgrade(getUpgradeWithData(stack));
        }
    }

    // IComputerItem implementation

    private static void setComputerID(ItemStack stack, int computerID) {
        stack.getOrCreateTag().putInt(NBT_ID, computerID);
    }

    @Override
    public @Nullable String getLabel(ItemStack stack) {
        return IComputerItem.super.getLabel(stack);
    }

    public ComputerFamily getFamily() {
        return family;
    }

    @Override
    public ItemStack changeItem(ItemStack stack, Item newItem) {
        return newItem instanceof PocketComputerItem pocket ? pocket.create(
            getComputerID(stack), getLabel(stack), getColour(stack),
            getUpgradeWithData(stack)
        ) : ItemStack.EMPTY;
    }

    // IMedia

    @Override
    public boolean setLabel(ItemStack stack, @Nullable String label) {
        if (label != null) {
            stack.setHoverName(Component.literal(label));
        } else {
            stack.resetHoverName();
        }
        return true;
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var id = getComputerID(stack);
        if (id >= 0) {
            return ComputerCraftAPI.createSaveDirMount(level.getServer(), "computer/" + id, Config.computerSpaceLimit);
        }
        return null;
    }

    public static @Nullable UUID getInstanceID(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.hasUUID(NBT_INSTANCE) ? nbt.getUUID(NBT_INSTANCE) : null;
    }

    private static int getSessionID(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_SESSION) ? nbt.getInt(NBT_SESSION) : -1;
    }

    private static boolean isMarkedOn(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.getBoolean(NBT_ON);
    }

    public static @Nullable IPocketUpgrade getUpgrade(ItemStack stack) {
        var compound = stack.getTag();
        if (compound == null || !compound.contains(NBT_UPGRADE)) return null;
        return PocketUpgrades.instance().get(compound.getString(NBT_UPGRADE));
    }

    public static @Nullable UpgradeData<IPocketUpgrade> getUpgradeWithData(ItemStack stack) {
        var compound = stack.getTag();
        if (compound == null || !compound.contains(NBT_UPGRADE)) return null;
        var upgrade = PocketUpgrades.instance().get(compound.getString(NBT_UPGRADE));
        return upgrade == null ? null : UpgradeData.of(upgrade, NBTUtil.getCompoundOrEmpty(compound, NBT_UPGRADE_INFO));
    }

    public static void setUpgrade(ItemStack stack, @Nullable UpgradeData<IPocketUpgrade> upgrade) {
        var compound = stack.getOrCreateTag();

        if (upgrade == null) {
            compound.remove(NBT_UPGRADE);
            compound.remove(NBT_UPGRADE_INFO);
        } else {
            compound.putString(NBT_UPGRADE, upgrade.upgrade().getUpgradeID().toString());
            compound.put(NBT_UPGRADE_INFO, upgrade.data().copy());
        }
    }

    public static CompoundTag getUpgradeInfo(ItemStack stack) {
        return stack.getOrCreateTagElement(NBT_UPGRADE_INFO);
    }
}
