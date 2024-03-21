// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.GuardedLuaContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public final class GenericPeripheral implements IDynamicPeripheral {
    private final BlockEntity tile;
    private final Direction side;

    private final String type;
    private final Set<String> additionalTypes;
    private final List<SaturatedMethod> methods;

    private @Nullable GuardedLuaContext contextWrapper;
    private final GuardedLuaContext.Guard guard;

    GenericPeripheral(BlockEntity tile, Direction side, String type, Set<String> additionalTypes, List<SaturatedMethod> methods) {
        this.side = side;
        this.tile = tile;
        this.type = type;
        this.additionalTypes = additionalTypes;
        this.methods = methods;
        this.guard = () -> !tile.isRemoved() && tile.getLevel() != null && tile.getLevel().isLoaded(tile.getBlockPos());
    }

    public Direction side() {
        return side;
    }

    @Override
    public String[] getMethodNames() {
        var names = new String[methods.size()];
        for (var i = 0; i < methods.size(); i++) names[i] = methods.get(i).getName();
        return names;
    }

    @Override
    public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments) throws LuaException {
        var contextWrapper = this.contextWrapper;
        if (contextWrapper == null || !contextWrapper.wraps(context)) {
            contextWrapper = this.contextWrapper = new GuardedLuaContext(context, guard);
        }

        return methods.get(method).apply(contextWrapper, computer, arguments);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return additionalTypes;
    }

    @Override
    public Object getTarget() {
        return tile;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (other == this) return true;
        if (!(other instanceof GenericPeripheral generic)) return false;

        return tile == generic.tile && methods.equals(generic.methods);
    }
}
