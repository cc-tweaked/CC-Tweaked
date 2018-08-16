package dan200.computercraft.shared.pocket.peripherals;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PocketModem implements IPocketUpgrade
{
    private final boolean m_advanced;

    public PocketModem( boolean m_advanced )
    {
        this.m_advanced = m_advanced;
    }

    @Nonnull
    @Override
    public ResourceLocation getUpgradeID()
    {
        return m_advanced
            ? new ResourceLocation( "computercraft", "advanved_modem" )
            : new ResourceLocation( "computercraft", "wireless_modem" );
    }

    @Nonnull
    @Override
    public String getUnlocalisedAdjective()
    {
        return m_advanced
            ? "upgrade.computercraft:advanced_modem.adjective"
            : "upgrade.computercraft:wireless_modem.adjective";
    }

    @Nonnull
    @Override
    public ItemStack getCraftingItem()
    {
        return new ItemStack( m_advanced ? ComputerCraft.Blocks.advancedModem : ComputerCraft.Blocks.wirelessModem );
    }

    @Nullable
    @Override
    public IPeripheral createPeripheral( @Nonnull IPocketAccess access )
    {
        return new PocketModemPeripheral( m_advanced );
    }

    @Override
    public void update( @Nonnull IPocketAccess access, @Nullable IPeripheral peripheral )
    {
        if( peripheral instanceof PocketModemPeripheral )
        {
            Entity entity = access.getEntity();

            PocketModemPeripheral modem = (PocketModemPeripheral) peripheral;
            if( entity instanceof EntityLivingBase )
            {
                EntityLivingBase player = (EntityLivingBase) entity;
                modem.setLocation( entity.getEntityWorld(), player.posX, player.posY + player.getEyeHeight(), player.posZ );
            }
            else if( entity != null )
            {
                modem.setLocation( entity.getEntityWorld(), entity.posX, entity.posY, entity.posZ );
            }

            access.setLight( modem.isActive() ? 0xBA0000 : -1 );
        }
    }
}
