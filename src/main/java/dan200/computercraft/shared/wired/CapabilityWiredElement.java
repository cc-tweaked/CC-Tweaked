/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.wired;

public final class CapabilityWiredElement
{
    /*
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
        public INBTBase writeNBT( Capability<IWiredElement> capability, IWiredElement instance, Direction side )
        {
            return null;
        }

        @Override
        public void readNBT( Capability<IWiredElement> capability, IWiredElement instance, Direction side, INBTBase base )
        {
        }
    }

    private static final IWiredElement NULL_ELEMENT = new NullElement();

    @Nullable
    public static IWiredElement unwrap( LazyOptional<IWiredElement> capability )
    {
        IWiredElement element = capability.orElse( NULL_ELEMENT );
        return element == NULL_ELEMENT ? null : element;
    }
    */
}
