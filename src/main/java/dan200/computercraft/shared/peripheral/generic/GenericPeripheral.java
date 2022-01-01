/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

class GenericPeripheral implements IDynamicPeripheral
{
    private final String type;
    private final Set<String> additionalTypes;
    private final BlockEntity tile;
    private final List<SaturatedMethod> methods;

    GenericPeripheral( BlockEntity tile, String name, Set<String> additionalTypes, List<SaturatedMethod> methods )
    {
        ResourceLocation type = tile.getType().getRegistryName();
        this.tile = tile;
        this.type = name != null ? name : (type != null ? type.toString() : "unknown");
        this.additionalTypes = additionalTypes;
        this.methods = methods;
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        String[] names = new String[methods.size()];
        for( int i = 0; i < methods.size(); i++ ) names[i] = methods.get( i ).getName();
        return names;
    }

    @Nonnull
    @Override
    public MethodResult callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull IArguments arguments ) throws LuaException
    {
        return methods.get( method ).apply( context, computer, arguments );
    }

    @Nonnull
    @Override
    public String getType()
    {
        return type;
    }

    @Nonnull
    @Override
    public Set<String> getAdditionalTypes()
    {
        return additionalTypes;
    }

    @Nullable
    @Override
    public Object getTarget()
    {
        return tile;
    }

    @Override
    public boolean equals( @Nullable IPeripheral other )
    {
        if( other == this ) return true;
        if( !(other instanceof GenericPeripheral generic) ) return false;

        return tile == generic.tile && methods.equals( generic.methods );
    }
}
