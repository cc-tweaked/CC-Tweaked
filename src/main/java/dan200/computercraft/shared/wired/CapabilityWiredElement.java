/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.wired;

import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nonnull;

public final class CapabilityWiredElement
{
    @CapabilityInject( IWiredElement.class )
    public static Capability<IWiredElement> CAPABILITY = null;

    private CapabilityWiredElement() {}

    public static void register()
    {
        CapabilityManager.INSTANCE.register( IWiredElement.class, new NullStorage(), NullElement::new );
    }

    private static class NullElement implements IWiredElement
    {
        @Nonnull
        @Override
        public IWiredNode getNode()
        {
            throw new IllegalStateException( "Should not use the default element implementation" );
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            throw new IllegalStateException( "Should not use the default element implementation" );
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            throw new IllegalStateException( "Should not use the default element implementation" );
        }

        @Nonnull
        @Override
        public String getSenderID()
        {
            throw new IllegalStateException( "Should not use the default element implementation" );
        }
    }

    private static class NullStorage implements Capability.IStorage<IWiredElement>
    {
        @Override
        public NBTBase writeNBT( Capability<IWiredElement> capability, IWiredElement instance, EnumFacing side )
        {
            return null;
        }

        @Override
        public void readNBT( Capability<IWiredElement> capability, IWiredElement instance, EnumFacing side, NBTBase base )
        {
        }
    }
}
