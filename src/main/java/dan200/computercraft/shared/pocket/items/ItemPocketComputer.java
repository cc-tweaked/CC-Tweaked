/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.items;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.pocket.inventory.PocketComputerMenuProvider;
import dan200.computercraft.shared.util.IDAssigner;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem
{
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    public static final String NBT_LIGHT = "Light";
    private static final String NBT_ON = "On";

    private static final String NBT_INSTANCE = "Instanceid";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;

    public ItemPocketComputer( Properties settings, ComputerFamily family )
    {
        super( settings );
        this.family = family;
    }

    public ItemStack create( int id, String label, int colour, IPocketUpgrade upgrade )
    {
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateTag().putInt( NBT_ID, id );
        if( label != null ) result.setHoverName( new StringTextComponent( label ) );
        if( upgrade != null ) result.getOrCreateTag().putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        if( colour != -1 ) result.getOrCreateTag().putInt( NBT_COLOUR, colour );
        return result;
    }

    @Override
    public void fillItemCategory( @Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> stacks )
    {
        if( !allowdedIn( group ) ) return;
        stacks.add( create( -1, null, -1, null ) );
        for( IPocketUpgrade upgrade : PocketUpgrades.getVanillaUpgrades() )
        {
            stacks.add( create( -1, null, -1, upgrade ) );
        }
    }

    private boolean tick( @Nonnull ItemStack stack, @Nonnull World world, @Nonnull Entity entity, @Nonnull PocketServerComputer computer )
    {
        IPocketUpgrade upgrade = getUpgrade( stack );

        computer.setWorld( (ServerWorld) world );
        computer.updateValues( entity, stack, upgrade );

        boolean changed = false;

        // Sync ID
        int id = computer.getID();
        if( id != getComputerID( stack ) )
        {
            changed = true;
            setComputerID( stack, id );
        }

        // Sync label
        String label = computer.getLabel();
        if( !Objects.equal( label, getLabel( stack ) ) )
        {
            changed = true;
            setLabel( stack, label );
        }

        boolean on = computer.isOn();
        if( on != isMarkedOn( stack ) )
        {
            changed = true;
            stack.getOrCreateTag().putBoolean( NBT_ON, on );
        }

        // Update pocket upgrade
        if( upgrade != null ) upgrade.update( computer, computer.getPeripheral( ComputerSide.BACK ) );

        return changed;
    }

    @Override
    public void inventoryTick( @Nonnull ItemStack stack, World world, @Nonnull Entity entity, int slotNum, boolean selected )
    {
        if( world.isClientSide ) return;
        IInventory inventory = entity instanceof PlayerEntity ? ((PlayerEntity) entity).inventory : null;
        PocketServerComputer computer = createServerComputer( (ServerWorld) world, entity, inventory, stack );
        computer.keepAlive();

        boolean changed = tick( stack, world, entity, computer );
        if( changed && inventory != null ) inventory.setChanged();
    }

    @Override
    public boolean onEntityItemUpdate( ItemStack stack, ItemEntity entity )
    {
        if( entity.level.isClientSide ) return false;

        PocketServerComputer computer = getServerComputer( entity.level.getServer(), stack );
        if( computer != null && tick( stack, entity.level, entity, computer ) ) entity.setItem( stack.copy() );
        return false;
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> use( World world, PlayerEntity player, @Nonnull Hand hand )
    {
        ItemStack stack = player.getItemInHand( hand );
        if( !world.isClientSide )
        {
            PocketServerComputer computer = createServerComputer( (ServerWorld) world, player, player.inventory, stack );
            computer.turnOn();

            boolean stop = false;
            IPocketUpgrade upgrade = getUpgrade( stack );
            if( upgrade != null )
            {
                computer.updateValues( player, stack, upgrade );
                stop = upgrade.onRightClick( world, computer, computer.getPeripheral( ComputerSide.BACK ) );
            }

            if( !stop )
            {
                boolean isTypingOnly = hand == Hand.OFF_HAND;
                new ComputerContainerData( computer, stack ).open( player, new PocketComputerMenuProvider( computer, stack, this, hand, isTypingOnly ) );
            }
        }
        return new ActionResult<>( ActionResultType.SUCCESS, stack );
    }

    @Nonnull
    @Override
    public ITextComponent getName( @Nonnull ItemStack stack )
    {
        String baseString = getDescriptionId( stack );
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            return new TranslationTextComponent( baseString + ".upgraded",
                new TranslationTextComponent( upgrade.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return super.getName( stack );
        }
    }


    @Override
    public void appendHoverText( @Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> list, ITooltipFlag flag )
    {
        if( flag.isAdvanced() || getLabel( stack ) == null )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                list.add( new TranslationTextComponent( "gui.computercraft.tooltip.computer_id", id )
                    .withStyle( TextFormatting.GRAY ) );
            }
        }
    }

    @Nullable
    @Override
    public String getCreatorModId( ItemStack stack )
    {
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            // If we're a non-vanilla, non-CC upgrade then return whichever mod this upgrade
            // belongs to.
            String mod = PocketUpgrades.getOwner( upgrade );
            if( mod != null && !mod.equals( ComputerCraft.MOD_ID ) ) return mod;
        }

        return super.getCreatorModId( stack );
    }

    @Nonnull
    public PocketServerComputer createServerComputer( ServerWorld world, Entity entity, @Nullable IInventory inventory, @Nonnull ItemStack stack )
    {
        if( world.isClientSide ) throw new IllegalStateException( "Cannot call createServerComputer on the client" );

        int sessionID = getSessionID( stack );

        ServerComputerRegistry registry = ServerContext.get( world.getServer() ).registry();
        PocketServerComputer computer = (PocketServerComputer) registry.get( sessionID, getInstanceID( stack ) );
        if( computer == null )
        {
            int computerID = getComputerID( stack );
            if( computerID < 0 )
            {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, IDAssigner.COMPUTER );
                setComputerID( stack, computerID );
            }

            computer = new PocketServerComputer( world, getComputerID( stack ), getLabel( stack ), getFamily() );

            setInstanceID( stack, computer.register() );
            setSessionID( stack, registry.getSessionID() );

            computer.updateValues( entity, stack, getUpgrade( stack ) );
            computer.addAPI( new PocketAPI( computer ) );

            // Only turn on when initially creating the computer, rather than each tick.
            if( isMarkedOn( stack ) && entity instanceof PlayerEntity ) computer.turnOn();

            if( inventory != null ) inventory.setChanged();
        }
        computer.setWorld( world );
        return computer;
    }

    @Nullable
    public static PocketServerComputer getServerComputer( MinecraftServer server, @Nonnull ItemStack stack )
    {
        return (PocketServerComputer) ServerContext.get( server ).registry().get( getSessionID( stack ), getInstanceID( stack ) );
    }

    // IComputerItem implementation

    private static void setComputerID( @Nonnull ItemStack stack, int computerID )
    {
        stack.getOrCreateTag().putInt( NBT_ID, computerID );
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return IComputerItem.super.getLabel( stack );
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return PocketComputerItemFactory.create(
            getComputerID( stack ), getLabel( stack ), getColour( stack ),
            family, getUpgrade( stack )
        );
    }

    // IMedia

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setHoverName( new StringTextComponent( label ) );
        }
        else
        {
            stack.resetHoverName();
        }
        return true;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            return ComputerCraftAPI.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
        }
        return null;
    }

    public static int getInstanceID( @Nonnull ItemStack stack )
    {
        CompoundNBT nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_INSTANCE ) ? nbt.getInt( NBT_INSTANCE ) : -1;
    }

    private static void setInstanceID( @Nonnull ItemStack stack, int instanceID )
    {
        stack.getOrCreateTag().putInt( NBT_INSTANCE, instanceID );
    }

    private static int getSessionID( @Nonnull ItemStack stack )
    {
        CompoundNBT nbt = stack.getTag();
        return nbt != null && nbt.contains( NBT_SESSION ) ? nbt.getInt( NBT_SESSION ) : -1;
    }

    private static void setSessionID( @Nonnull ItemStack stack, int sessionID )
    {
        stack.getOrCreateTag().putInt( NBT_SESSION, sessionID );
    }

    private static boolean isMarkedOn( @Nonnull ItemStack stack )
    {
        CompoundNBT nbt = stack.getTag();
        return nbt != null && nbt.getBoolean( NBT_ON );
    }

    public static IPocketUpgrade getUpgrade( @Nonnull ItemStack stack )
    {
        CompoundNBT compound = stack.getTag();
        return compound != null && compound.contains( NBT_UPGRADE )
            ? PocketUpgrades.get( compound.getString( NBT_UPGRADE ) ) : null;

    }

    public static void setUpgrade( @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        CompoundNBT compound = stack.getOrCreateTag();

        if( upgrade == null )
        {
            compound.remove( NBT_UPGRADE );
        }
        else
        {
            compound.putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        }

        compound.remove( NBT_UPGRADE_INFO );
    }

    public static CompoundNBT getUpgradeInfo( @Nonnull ItemStack stack )
    {
        return stack.getOrCreateTagElement( NBT_UPGRADE_INFO );
    }
}
