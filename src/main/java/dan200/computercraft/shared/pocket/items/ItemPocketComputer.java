/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.items;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem {
    public static final String NBT_LIGHT = "Light";
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    private static final String NBT_INSTANCE = "Instanceid";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;

    public ItemPocketComputer(Settings settings, ComputerFamily family) {
        super(settings);
        this.family = family;
    }

    public static ServerComputer getServerComputer(@Nonnull ItemStack stack) {
        int session = getSessionID( stack );
        if( session != ComputerCraft.serverComputerRegistry.getSessionID() ) return null;

        int instanceID = getInstanceID(stack);
        return instanceID >= 0 ? ComputerCraft.serverComputerRegistry.get(instanceID) : null;
    }

    @Environment (EnvType.CLIENT)
    public static ComputerState getState(@Nonnull ItemStack stack) {
        ClientComputer computer = getClientComputer(stack);
        return computer == null ? ComputerState.OFF : computer.getState();
    }

    private static ClientComputer getClientComputer(@Nonnull ItemStack stack) {
        int instanceID = getInstanceID(stack);
        return instanceID >= 0 ? ComputerCraft.clientComputerRegistry.get(instanceID) : null;
    }

    @Environment (EnvType.CLIENT)
    public static int getLightState(@Nonnull ItemStack stack) {
        ClientComputer computer = getClientComputer(stack);
        if (computer != null && computer.isOn()) {
            CompoundTag computerNBT = computer.getUserData();
            if (computerNBT != null && computerNBT.contains(NBT_LIGHT)) {
                return computerNBT.getInt(NBT_LIGHT);
            }
        }
        return -1;
    }

    public static void setUpgrade(@Nonnull ItemStack stack, IPocketUpgrade upgrade) {
        CompoundTag compound = stack.getOrCreateTag();

        if (upgrade == null) {
            compound.remove(NBT_UPGRADE);
        } else {
            compound.putString(NBT_UPGRADE,
                               upgrade.getUpgradeID()
                                      .toString());
        }

        compound.remove(NBT_UPGRADE_INFO);
    }

    public static CompoundTag getUpgradeInfo(@Nonnull ItemStack stack) {
        return stack.getOrCreateSubTag(NBT_UPGRADE_INFO);
    }

    //    @Nullable
    //    @Override
    //    public String getCreatorModId( ItemStack stack )
    //    {
    //        IPocketUpgrade upgrade = getUpgrade( stack );
    //        if( upgrade != null )
    //        {
    //            // If we're a non-vanilla, non-CC upgrade then return whichever mod this upgrade
    //            // belongs to.
    //            String mod = PocketUpgrades.getOwner( upgrade );
    //            if( mod != null && !mod.equals( ComputerCraft.MOD_ID ) ) return mod;
    //        }
    //
    //        return super.getCreatorModId( stack );
    //    }

    @Nonnull
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, @Nonnull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            PocketServerComputer computer = this.createServerComputer(world, player.inventory, player, stack);

            boolean stop = false;
            if (computer != null) {
                computer.turnOn();

                IPocketUpgrade upgrade = getUpgrade(stack);
                if (upgrade != null) {
                    computer.updateValues(player, stack, upgrade);
                    stop = upgrade.onRightClick(world, computer, computer.getPeripheral(ComputerSide.BACK));
                }
            }

            if (!stop && computer != null) {
                computer.sendTerminalState(player);
                new ComputerContainerData(computer).open(player, new ContainerPocketComputer.Factory(computer, stack, this, hand));
            }
        }
        return new TypedActionResult<>(ActionResult.SUCCESS, stack);
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, World world, @Nonnull Entity entity, int slotNum, boolean selected) {
        if (!world.isClient) {
            // Server side
            Inventory inventory = entity instanceof PlayerEntity ? ((PlayerEntity) entity).inventory : null;
            PocketServerComputer computer = this.createServerComputer(world, inventory, entity, stack);
            if (computer != null) {
                IPocketUpgrade upgrade = getUpgrade(stack);

                // Ping computer
                computer.keepAlive();
                computer.setWorld(world);
                computer.updateValues(entity, stack, upgrade);

                // Sync ID
                int id = computer.getID();
                if (id != this.getComputerID(stack)) {
                    setComputerID(stack, id);
                    if (inventory != null) {
                        inventory.markDirty();
                    }
                }

                // Sync label
                String label = computer.getLabel();
                if (!Objects.equal(label, this.getLabel(stack))) {
                    this.setLabel(stack, label);
                    if (inventory != null) {
                        inventory.markDirty();
                    }
                }

                // Update pocket upgrade
                if (upgrade != null) {
                    upgrade.update(computer, computer.getPeripheral(ComputerSide.BACK));
                }
            }
        } else {
            // Client side
            createClientComputer(stack);
        }
    }

    @Override
    public void appendTooltip(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<Text> list, TooltipContext flag) {
        if (flag.isAdvanced() || this.getLabel(stack) == null) {
            int id = this.getComputerID(stack);
            if (id >= 0) {
                list.add(new TranslatableText("gui.computercraft.tooltip.computer_id", id).formatted(Formatting.GRAY));
            }
        }
    }

    @Nonnull
    @Override
    public Text getName(@Nonnull ItemStack stack) {
        String baseString = this.getTranslationKey(stack);
        IPocketUpgrade upgrade = getUpgrade(stack);
        if (upgrade != null) {
            return new TranslatableText(baseString + ".upgraded", new TranslatableText(upgrade.getUnlocalisedAdjective()));
        } else {
            return super.getName(stack);
        }
    }

    // IComputerItem implementation

    @Override
    public void appendStacks(@Nonnull ItemGroup group, @Nonnull DefaultedList<ItemStack> stacks) {
        if (!this.isIn(group)) {
            return;
        }
        stacks.add(this.create(-1, null, -1, null));
        for (IPocketUpgrade upgrade : PocketUpgrades.getVanillaUpgrades()) {
            stacks.add(this.create(-1, null, -1, upgrade));
        }
    }

    public ItemStack create(int id, String label, int colour, IPocketUpgrade upgrade) {
        ItemStack result = new ItemStack(this);
        if (id >= 0) {
            result.getOrCreateTag()
                  .putInt(NBT_ID, id);
        }
        if (label != null) {
            result.setCustomName(new LiteralText(label));
        }
        if (upgrade != null) {
            result.getOrCreateTag()
                  .putString(NBT_UPGRADE,
                             upgrade.getUpgradeID()
                                    .toString());
        }
        if (colour != -1) {
            result.getOrCreateTag()
                  .putInt(NBT_COLOUR, colour);
        }
        return result;
    }

    public PocketServerComputer createServerComputer(final World world, Inventory inventory, Entity entity, @Nonnull ItemStack stack) {
        if (world.isClient) {
            return null;
        }

        PocketServerComputer computer;
        int instanceID = getInstanceID(stack);
        int sessionID = getSessionID(stack);
        int correctSessionID = ComputerCraft.serverComputerRegistry.getSessionID();

        if (instanceID >= 0 && sessionID == correctSessionID && ComputerCraft.serverComputerRegistry.contains(instanceID)) {
            computer = (PocketServerComputer) ComputerCraft.serverComputerRegistry.get(instanceID);
        } else {
            if (instanceID < 0 || sessionID != correctSessionID) {
                instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
                setInstanceID(stack, instanceID);
                setSessionID(stack, correctSessionID);
            }
            int computerID = this.getComputerID(stack);
            if (computerID < 0) {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir(world, "computer");
                setComputerID(stack, computerID);
            }
            computer = new PocketServerComputer(world, computerID, this.getLabel(stack), instanceID, this.getFamily());
            computer.updateValues(entity, stack, getUpgrade(stack));
            computer.addAPI(new PocketAPI(computer));
            ComputerCraft.serverComputerRegistry.add(instanceID, computer);
            if (inventory != null) {
                inventory.markDirty();
            }
        }
        computer.setWorld(world);
        return computer;
    }

    public static IPocketUpgrade getUpgrade(@Nonnull ItemStack stack) {
        CompoundTag compound = stack.getTag();
        return compound != null && compound.contains(NBT_UPGRADE) ? PocketUpgrades.get(compound.getString(NBT_UPGRADE)) : null;

    }

    // IMedia

    private static void setComputerID(@Nonnull ItemStack stack, int computerID) {
        stack.getOrCreateTag()
             .putInt(NBT_ID, computerID);
    }

    @Override
    public String getLabel(@Nonnull ItemStack stack) {
        return IComputerItem.super.getLabel(stack);
    }

    @Override
    public ComputerFamily getFamily() {
        return this.family;
    }

    @Override
    public ItemStack withFamily(@Nonnull ItemStack stack, @Nonnull ComputerFamily family) {
        return PocketComputerItemFactory.create(this.getComputerID(stack), this.getLabel(stack), this.getColour(stack), family, getUpgrade(stack));
    }

    @Override
    public boolean setLabel(@Nonnull ItemStack stack, String label) {
        if (label != null) {
            stack.setCustomName(new LiteralText(label));
        } else {
            stack.removeCustomName();
        }
        return true;
    }

    public static ClientComputer createClientComputer(@Nonnull ItemStack stack) {
        int instanceID = getInstanceID(stack);
        if (instanceID >= 0) {
            if (!ComputerCraft.clientComputerRegistry.contains(instanceID)) {
                ComputerCraft.clientComputerRegistry.add(instanceID, new ClientComputer(instanceID));
            }
            return ComputerCraft.clientComputerRegistry.get(instanceID);
        }
        return null;
    }

    private static int getInstanceID(@Nonnull ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_INSTANCE) ? nbt.getInt(NBT_INSTANCE) : -1;
    }

    private static int getSessionID(@Nonnull ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_SESSION) ? nbt.getInt(NBT_SESSION) : -1;
    }

    private static void setInstanceID(@Nonnull ItemStack stack, int instanceID) {
        stack.getOrCreateTag()
             .putInt(NBT_INSTANCE, instanceID);
    }

    private static void setSessionID(@Nonnull ItemStack stack, int sessionID) {
        stack.getOrCreateTag()
             .putInt(NBT_SESSION, sessionID);
    }

    @Override
    public IMount createDataMount(@Nonnull ItemStack stack, @Nonnull World world) {
        int id = this.getComputerID(stack);
        if (id >= 0) {
            return ComputerCraftAPI.createSaveDirMount(world, "computer/" + id, ComputerCraft.computerSpaceLimit);
        }
        return null;
    }
}
