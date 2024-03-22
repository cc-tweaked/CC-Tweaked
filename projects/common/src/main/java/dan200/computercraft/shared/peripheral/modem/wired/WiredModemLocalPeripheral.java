// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.util.PeripheralHelpers;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.platform.ComponentAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * Represents a local peripheral exposed on the wired network.
 * <p>
 * This is responsible for getting the peripheral in world, tracking id and type and determining whether
 * it has changed.
 */
public final class WiredModemLocalPeripheral {
    private static final String NBT_PERIPHERAL_TYPE = "PeripheralType";
    private static final String NBT_PERIPHERAL_ID = "PeripheralId";

    private int id = -1;
    private @Nullable String type;

    private @Nullable IPeripheral peripheral;
    private final ComponentAccess<IPeripheral> peripherals;

    public WiredModemLocalPeripheral(ComponentAccess<IPeripheral> peripherals) {
        this.peripherals = peripherals;
    }

    /**
     * Attach a new peripheral from the world.
     *
     * @param world     The world to search in
     * @param origin    The position to search from
     * @param direction The direction so search in
     * @return Whether the peripheral changed.
     */
    public boolean attach(Level world, BlockPos origin, Direction direction) {
        var oldPeripheral = peripheral;
        var peripheral = this.peripheral = getPeripheralFrom(world, origin, direction);

        if (peripheral == null) {
            return oldPeripheral != null;
        } else {
            var type = peripheral.getType();
            var id = this.id;

            if (id > 0 && this.type == null) {
                // If we had an ID but no type, then just set the type.
                this.type = type;
            } else if (id < 0 || !type.equals(this.type)) {
                this.type = type;
                this.id = ServerContext.get(assertNonNull(world.getServer())).getNextId("peripheral." + type);
            }

            return !PeripheralHelpers.equals(oldPeripheral, peripheral);
        }
    }

    /**
     * Detach the current peripheral.
     *
     * @return Whether the peripheral changed
     */
    public boolean detach() {
        if (peripheral == null) return false;
        peripheral = null;
        return true;
    }

    @Nullable
    public String getConnectedName() {
        return peripheral != null ? type + "_" + id : null;
    }

    public boolean hasPeripheral() {
        return peripheral != null;
    }

    public void extendMap(Map<String, IPeripheral> peripherals) {
        if (peripheral != null) peripherals.put(type + "_" + id, peripheral);
    }

    public Map<String, IPeripheral> toMap() {
        return peripheral == null ? Map.of() : Map.of(type + "_" + id, peripheral);
    }

    public void write(CompoundTag tag, String suffix) {
        if (id >= 0) tag.putInt(NBT_PERIPHERAL_ID + suffix, id);
        if (type != null) tag.putString(NBT_PERIPHERAL_TYPE + suffix, type);
    }

    public void read(CompoundTag tag, String suffix) {
        id = tag.contains(NBT_PERIPHERAL_ID + suffix, Tag.TAG_ANY_NUMERIC)
            ? tag.getInt(NBT_PERIPHERAL_ID + suffix) : -1;

        type = tag.contains(NBT_PERIPHERAL_TYPE + suffix, Tag.TAG_STRING)
            ? tag.getString(NBT_PERIPHERAL_TYPE + suffix) : null;
    }

    @Nullable
    private IPeripheral getPeripheralFrom(Level world, BlockPos pos, Direction direction) {
        var offset = pos.relative(direction);

        if (world.getBlockState(offset).is(ComputerCraftTags.Blocks.PERIPHERAL_HUB_IGNORE)) return null;

        var peripheral = peripherals.get(direction);
        return peripheral instanceof WiredModemPeripheral ? null : peripheral;
    }
}
