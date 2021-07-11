/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

class GenericPeripheral implements IDynamicPeripheral
{
    private final String type;
    private final BlockEntity tile;
    private final List<SaturatedMethod> methods;

    GenericPeripheral( BlockEntity tile, List<SaturatedMethod> methods )
    {
        Identifier type = BlockEntityType.getId( tile.getType() );
        this.tile = tile;
        this.type = type == null ? "unknown" : type.toString();
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
        if( !(other instanceof GenericPeripheral) ) return false;

        GenericPeripheral generic = (GenericPeripheral) other;
        return tile == generic.tile && methods.equals( generic.methods );
    }
}
