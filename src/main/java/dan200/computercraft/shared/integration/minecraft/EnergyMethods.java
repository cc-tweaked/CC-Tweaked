/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.minecraft;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.asm.GenericSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.versions.forge.ForgeVersion;

import javax.annotation.Nonnull;

@AutoService( GenericSource.class )
public class EnergyMethods implements GenericSource
{
    @Nonnull
    @Override
    public ResourceLocation id()
    {
        return new ResourceLocation( ForgeVersion.MOD_ID, "energy" );
    }

    @LuaFunction( mainThread = true )
    public static int getEnergy( IEnergyStorage energy )
    {
        return energy.getEnergyStored();
    }

    @LuaFunction( mainThread = true )
    public static int getEnergyCapacity( IEnergyStorage energy )
    {
        return energy.getMaxEnergyStored();
    }
}
