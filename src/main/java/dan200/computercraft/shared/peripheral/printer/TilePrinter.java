/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public final class TilePrinter extends TileGeneric implements DefaultSidedInventory, INameable, INamedContainerProvider
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_PRINTING = "Printing";
    private static final String NBT_PAGE_TITLE = "PageTitle";

    static final int SLOTS = 13;

    private static final int[] BOTTOM_SLOTS = new int[] { 7, 8, 9, 10, 11, 12 };
    private static final int[] TOP_SLOTS = new int[] { 1, 2, 3, 4, 5, 6 };
    private static final int[] SIDE_SLOTS = new int[] { 0 };

    ITextComponent customName;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize( SLOTS, ItemStack.EMPTY );
    private final SidedCaps<IItemHandler> itemHandlerCaps =
        SidedCaps.ofNullable( facing -> facing == null ? new InvWrapper( this ) : new SidedInvWrapper( this, facing ) );
    private LazyOptional<IPeripheral> peripheralCap;

    private final Terminal page = new Terminal( ItemPrintout.LINE_MAX_LENGTH, ItemPrintout.LINES_PER_PAGE );
    private String pageTitle = "";
    private boolean printing = false;

    public TilePrinter( TileEntityType<TilePrinter> type )
    {
        super( type );
    }

    @Override
    public void destroy()
    {
        ejectContents();
    }

    @Override
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        itemHandlerCaps.invalidate();
        peripheralCap = CapabilityUtil.invalidate( peripheralCap );
    }

    @Nonnull
    @Override
    public ActionResultType onActivate( PlayerEntity player, Hand hand, BlockRayTraceResult hit )
    {
        if( player.isCrouching() ) return ActionResultType.PASS;

        if( !getLevel().isClientSide ) NetworkHooks.openGui( (ServerPlayerEntity) player, this );
        return ActionResultType.SUCCESS;
    }

    @Override
    public void load( @Nonnull CompoundNBT nbt )
    {
        super.load( nbt );

        customName = nbt.contains( NBT_NAME ) ? ITextComponent.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;

        // Read page
        synchronized( page )
        {
            printing = nbt.getBoolean( NBT_PRINTING );
            pageTitle = nbt.getString( NBT_PAGE_TITLE );
            page.readFromNBT( nbt );
        }

        // Read inventory
        ItemStackHelper.loadAllItems( nbt, inventory );
    }

    @Nonnull
    @Override
    public CompoundNBT save( @Nonnull CompoundNBT nbt )
    {
        if( customName != null ) nbt.putString( NBT_NAME, ITextComponent.Serializer.toJson( customName ) );

        // Write page
        synchronized( page )
        {
            nbt.putBoolean( NBT_PRINTING, printing );
            nbt.putString( NBT_PAGE_TITLE, pageTitle );
            page.writeToNBT( nbt );
        }

        // Write inventory
        ItemStackHelper.saveAllItems( nbt, inventory );

        return super.save( nbt );
    }

    boolean isPrinting()
    {
        return printing;
    }

    // IInventory implementation
    @Override
    public int getContainerSize()
    {
        return inventory.size();
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : inventory )
        {
            if( !stack.isEmpty() ) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getItem( int slot )
    {
        return inventory.get( slot );
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate( int slot )
    {
        ItemStack result = inventory.get( slot );
        inventory.set( slot, ItemStack.EMPTY );
        setChanged();
        updateBlockState();
        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeItem( int slot, int count )
    {
        ItemStack stack = inventory.get( slot );
        if( stack.isEmpty() ) return ItemStack.EMPTY;

        if( stack.getCount() <= count )
        {
            setItem( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.split( count );
        if( inventory.get( slot ).isEmpty() )
        {
            inventory.set( slot, ItemStack.EMPTY );
            updateBlockState();
        }
        setChanged();
        return part;
    }

    @Override
    public void setItem( int slot, @Nonnull ItemStack stack )
    {
        inventory.set( slot, stack );
        setChanged();
        updateBlockState();
    }

    @Override
    public void clearContent()
    {
        for( int i = 0; i < inventory.size(); i++ ) inventory.set( i, ItemStack.EMPTY );
        setChanged();
        updateBlockState();
    }

    @Override
    public boolean canPlaceItem( int slot, @Nonnull ItemStack stack )
    {
        if( slot == 0 )
        {
            return isInk( stack );
        }
        else if( slot >= TOP_SLOTS[0] && slot <= TOP_SLOTS[TOP_SLOTS.length - 1] )
        {
            return isPaper( stack );
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean stillValid( @Nonnull PlayerEntity playerEntity )
    {
        return isUsable( playerEntity, false );
    }

    // ISidedInventory implementation

    @Nonnull
    @Override
    public int[] getSlotsForFace( @Nonnull Direction side )
    {
        switch( side )
        {
            case DOWN: // Bottom (Out tray)
                return BOTTOM_SLOTS;
            case UP: // Top (In tray)
                return TOP_SLOTS;
            default: // Sides (Ink)
                return SIDE_SLOTS;
        }
    }

    @Nullable
    Terminal getCurrentPage()
    {
        synchronized( page )
        {
            return printing ? page : null;
        }
    }

    boolean startNewPage()
    {
        synchronized( page )
        {
            if( !canInputPage() ) return false;
            if( printing && !outputPage() ) return false;
            return inputPage();
        }
    }

    boolean endCurrentPage()
    {
        synchronized( page )
        {
            return printing && outputPage();
        }
    }

    int getInkLevel()
    {
        ItemStack inkStack = inventory.get( 0 );
        return isInk( inkStack ) ? inkStack.getCount() : 0;
    }

    int getPaperLevel()
    {
        int count = 0;
        for( int i = 1; i < 7; i++ )
        {
            ItemStack paperStack = inventory.get( i );
            if( isPaper( paperStack ) ) count += paperStack.getCount();
        }
        return count;
    }

    void setPageTitle( String title )
    {
        synchronized( page )
        {
            if( printing ) pageTitle = title;
        }
    }

    static boolean isInk( @Nonnull ItemStack stack )
    {
        return ColourUtils.getStackColour( stack ) != null;
    }

    private static boolean isPaper( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        return item == Items.PAPER
            || (item instanceof ItemPrintout && ((ItemPrintout) item).getType() == ItemPrintout.Type.PAGE);
    }

    private boolean canInputPage()
    {
        ItemStack inkStack = inventory.get( 0 );
        return !inkStack.isEmpty() && isInk( inkStack ) && getPaperLevel() > 0;
    }

    private boolean inputPage()
    {
        ItemStack inkStack = inventory.get( 0 );
        DyeColor dye = ColourUtils.getStackColour( inkStack );
        if( dye == null ) return false;

        for( int i = 1; i < 7; i++ )
        {
            ItemStack paperStack = inventory.get( i );
            if( paperStack.isEmpty() || !isPaper( paperStack ) ) continue;

            // Setup the new page
            page.setTextColour( dye.getId() );

            page.clear();
            if( paperStack.getItem() instanceof ItemPrintout )
            {
                pageTitle = ItemPrintout.getTitle( paperStack );
                String[] text = ItemPrintout.getText( paperStack );
                String[] textColour = ItemPrintout.getColours( paperStack );
                for( int y = 0; y < page.getHeight(); y++ )
                {
                    page.setLine( y, text[y], textColour[y], "" );
                }
            }
            else
            {
                pageTitle = "";
            }
            page.setCursorPos( 0, 0 );

            // Decrement ink
            inkStack.shrink( 1 );
            if( inkStack.isEmpty() ) inventory.set( 0, ItemStack.EMPTY );

            // Decrement paper
            paperStack.shrink( 1 );
            if( paperStack.isEmpty() )
            {
                inventory.set( i, ItemStack.EMPTY );
                updateBlockState();
            }

            setChanged();
            printing = true;
            return true;
        }
        return false;
    }

    private boolean outputPage()
    {
        int height = page.getHeight();
        String[] lines = new String[height];
        String[] colours = new String[height];
        for( int i = 0; i < height; i++ )
        {
            lines[i] = page.getLine( i ).toString();
            colours[i] = page.getTextColourLine( i ).toString();
        }

        ItemStack stack = ItemPrintout.createSingleFromTitleAndText( pageTitle, lines, colours );
        for( int slot : BOTTOM_SLOTS )
        {
            if( inventory.get( slot ).isEmpty() )
            {
                setItem( slot, stack );
                printing = false;
                return true;
            }
        }
        return false;
    }

    private void ejectContents()
    {
        for( int i = 0; i < 13; i++ )
        {
            ItemStack stack = inventory.get( i );
            if( !stack.isEmpty() )
            {
                // Remove the stack from the inventory
                setItem( i, ItemStack.EMPTY );

                // Spawn the item in the world
                WorldUtil.dropItemStack( stack, getLevel(), new Vec3d( getBlockPos() ).add( 0.5, 0.75, 0.5 ) );
            }
        }
    }

    private void updateBlockState()
    {
        boolean top = false, bottom = false;
        for( int i = 1; i < 7; i++ )
        {
            ItemStack stack = inventory.get( i );
            if( !stack.isEmpty() && isPaper( stack ) )
            {
                top = true;
                break;
            }
        }
        for( int i = 7; i < 13; i++ )
        {
            ItemStack stack = inventory.get( i );
            if( !stack.isEmpty() && isPaper( stack ) )
            {
                bottom = true;
                break;
            }
        }

        updateBlockState( top, bottom );
    }

    private void updateBlockState( boolean top, boolean bottom )
    {
        if( remove ) return;

        BlockState state = getBlockState();
        if( state.getValue( BlockPrinter.TOP ) == top & state.getValue( BlockPrinter.BOTTOM ) == bottom ) return;

        getLevel().setBlockAndUpdate( getBlockPos(), state.setValue( BlockPrinter.TOP, top ).setValue( BlockPrinter.BOTTOM, bottom ) );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> capability, @Nullable Direction facing )
    {
        if( capability == ITEM_HANDLER_CAPABILITY ) return itemHandlerCaps.get( facing ).cast();
        if( capability == CAPABILITY_PERIPHERAL )
        {
            if( peripheralCap == null ) peripheralCap = LazyOptional.of( () -> new PrinterPeripheral( this ) );
            return peripheralCap.cast();
        }

        return super.getCapability( capability, facing );
    }

    @Override
    public boolean hasCustomName()
    {
        return customName != null;
    }

    @Nullable
    @Override
    public ITextComponent getCustomName()
    {
        return customName;
    }

    @Nonnull
    @Override
    public ITextComponent getName()
    {
        return customName != null ? customName : new TranslationTextComponent( getBlockState().getBlock().getDescriptionId() );
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return INameable.super.getDisplayName();
    }

    @Nonnull
    @Override
    public Container createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerPrinter( id, inventory, this );
    }
}
