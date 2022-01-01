/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.IMediaProvider;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class MediaProviders
{
    private static final Set<IMediaProvider> providers = new LinkedHashSet<>();

    private MediaProviders() {}

    public static synchronized void register( @Nonnull IMediaProvider provider )
    {
        Objects.requireNonNull( provider, "provider cannot be null" );
        providers.add( provider );
    }

    public static IMedia get( @Nonnull ItemStack stack )
    {
        if( stack.isEmpty() ) return null;

        // Try the handlers in order:
        for( IMediaProvider mediaProvider : providers )
        {
            try
            {
                IMedia media = mediaProvider.getMedia( stack );
                if( media != null ) return media;
            }
            catch( Exception e )
            {
                // mod misbehaved, ignore it
                ComputerCraft.log.error( "Media provider " + mediaProvider + " errored.", e );
            }
        }
        return null;
    }
}
