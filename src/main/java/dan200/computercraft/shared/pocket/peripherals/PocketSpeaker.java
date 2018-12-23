/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2016. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.PeripheralItemFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PocketSpeaker implements IPocketUpgrade
{
    public PocketSpeaker()
    {
    }

    @Nonnull
    @Override
    public ResourceLocation getUpgradeID()
    {
        return new ResourceLocation( "computercraft", "speaker" );
    }

    @Nonnull
    @Override
    public String getUnlocalisedAdjective()
    {
        return "upgrade.computercraft:speaker.adjective";
    }

    @Nonnull
    @Override
    public ItemStack getCraftingItem()
    {
        return PeripheralItemFactory.create( PeripheralType.Speaker, null, 1 );
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral( @Nonnull IPocketAccess access )
    {
        return new PocketSpeakerPeripheral();
    }

    @Override
    public void update( @Nonnull IPocketAccess access, @Nullable IPeripheral peripheral )
    {
        if( peripheral instanceof PocketSpeakerPeripheral )
        {
            Entity entity = access.getEntity();

            PocketSpeakerPeripheral speaker = (PocketSpeakerPeripheral) peripheral;

            if( entity instanceof EntityLivingBase )
            {
                EntityLivingBase player = (EntityLivingBase) entity;
                speaker.setLocation( entity.getEntityWorld(), player.posX, player.posY + player.getEyeHeight(), player.posZ );
            }

            else if( entity != null )
            {
                speaker.setLocation( entity.getEntityWorld(), entity.posX, entity.posY, entity.posZ );
            }
            speaker.update();
            access.setLight( speaker.madeSound( 20 ) ? 0x3320fc : -1 );
        }
    }
}
