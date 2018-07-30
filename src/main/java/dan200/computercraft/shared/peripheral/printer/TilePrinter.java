/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.peripheral.common.IPeripheralTile;
import dan200.computercraft.shared.util.IDefaultInventory;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

public class TilePrinter extends TileGeneric implements ISidedInventory, IDefaultInventory, IPeripheralTile
{
    private static final int[] bottomSlots = { 7, 8, 9, 10, 11, 12 };
    private static final int[] topSlots = { 1, 2, 3, 4, 5, 6 };
    private static final int[] sideSlots = { 0 };

    private final NonNullList<ItemStack> m_inventory;
    private final IItemHandlerModifiable m_itemHandlerAll = new InvWrapper( this );
    private IItemHandlerModifiable[] m_itemHandlerSides;

    private final Terminal m_page;
    private String m_pageTitle;
    private boolean m_printing;

    private String label;
    private boolean bottomFull;
    private boolean topFull;

    public TilePrinter()
    {
        m_inventory = NonNullList.withSize( 13, ItemStack.EMPTY );
        m_page = new Terminal( ItemPrintout.LINE_MAX_LENGTH, ItemPrintout.LINES_PER_PAGE );
        m_pageTitle = "";
        m_printing = false;
    }

    @Override
    public void destroy()
    {
        ejectContents();
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( !player.isSneaking() )
        {
            if( !getWorld().isRemote )
            {
                ComputerCraft.openPrinterGUI( player, this );
            }
            return true;
        }
        return false;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        updateAnim();
    }

    @Override
    public void readFromNBT( NBTTagCompound tag )
    {
        super.readFromNBT( tag );

        label = tag.hasKey( "label" ) ? tag.getString( "label" ) : null;

        // Read page
        synchronized( m_page )
        {
            m_printing = tag.getBoolean( "printing" );
            m_pageTitle = tag.getString( "pageTitle" );
            m_page.readFromNBT( tag );
        }

        // Read inventory
        synchronized( m_inventory )
        {
            NBTTagList nbttaglist = tag.getTagList( "Items", Constants.NBT.TAG_COMPOUND );
            for( int i = 0; i < nbttaglist.tagCount(); ++i )
            {
                NBTTagCompound itemTag = nbttaglist.getCompoundTagAt( i );
                int j = itemTag.getByte( "Slot" ) & 0xff;
                if( j >= 0 && j < m_inventory.size() )
                {
                    m_inventory.set( j, new ItemStack( itemTag ) );
                }
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound tag )
    {
        tag = super.writeToNBT( tag );

        if( label != null ) tag.setString( "label", label );

        // Write page
        synchronized( m_page )
        {
            tag.setBoolean( "printing", m_printing );
            tag.setString( "pageTitle", m_pageTitle );
            m_page.writeToNBT( tag );
        }

        // Write inventory
        synchronized( m_inventory )
        {
            NBTTagList nbttaglist = new NBTTagList();
            for( int i = 0; i < m_inventory.size(); ++i )
            {
                if( !m_inventory.get( i ).isEmpty() )
                {
                    NBTTagCompound itemtag = new NBTTagCompound();
                    itemtag.setByte( "Slot", (byte) i );
                    m_inventory.get( i ).writeToNBT( itemtag );
                    nbttaglist.appendTag( itemtag );
                }
            }
            tag.setTag( "Items", nbttaglist );
        }

        return tag;
    }

    @Override
    public final void readDescription( @Nonnull NBTTagCompound tag )
    {
        super.readDescription( tag );
        bottomFull = tag.getBoolean( "bottom_full" );
        topFull = tag.getBoolean( "top_full" );
        label = tag.hasKey( "label" ) ? tag.getString( "label" ) : null;

        getWorld().markBlockRangeForRenderUpdate( getPos(), getPos() );
    }

    @Override
    protected void writeDescription( @Nonnull NBTTagCompound tag )
    {
        super.writeDescription( tag );
        tag.setBoolean( "bottom_full", bottomFull );
        tag.setBoolean( "top_full", topFull );
        if( label != null ) tag.setString( "label", label );
    }

    public boolean isPrinting()
    {
        return m_printing;
    }

    // IInventory implementation

    @Override
    public int getSizeInventory()
    {
        return m_inventory.size();
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : m_inventory )
        {
            if( !stack.isEmpty() ) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot( int i )
    {
        synchronized( m_inventory )
        {
            return m_inventory.get( i );
        }
    }

    @Nonnull
    @Override
    public ItemStack removeStackFromSlot( int i )
    {
        synchronized( m_inventory )
        {
            ItemStack result = m_inventory.get( i );
            m_inventory.set( i, ItemStack.EMPTY );
            updateAnim();
            return result;
        }
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize( int i, int j )
    {
        synchronized( m_inventory )
        {
            if( m_inventory.get( i ).isEmpty() )
            {
                return ItemStack.EMPTY;
            }

            if( m_inventory.get( i ).getCount() <= j )
            {
                ItemStack itemstack = m_inventory.get( i );
                m_inventory.set( i, ItemStack.EMPTY );
                markDirty();
                updateAnim();
                return itemstack;
            }

            ItemStack part = m_inventory.get( i ).splitStack( j );
            if( m_inventory.get( i ).isEmpty() )
            {
                m_inventory.set( i, ItemStack.EMPTY );
                updateAnim();
            }
            markDirty();
            return part;
        }
    }

    @Override
    public void setInventorySlotContents( int i, @Nonnull ItemStack stack )
    {
        synchronized( m_inventory )
        {
            m_inventory.set( i, stack );
            markDirty();
            updateAnim();
        }
    }

    @Override
    public void clear()
    {
        synchronized( m_inventory )
        {
            for( int i = 0; i < m_inventory.size(); ++i )
            {
                m_inventory.set( i, ItemStack.EMPTY );
            }
            markDirty();
            updateAnim();
        }
    }

    @Override
    public boolean hasCustomName()
    {
        return getLabel() != null;
    }

    @Nonnull
    @Override
    public String getName()
    {
        String label = getLabel();
        if( label != null )
        {
            return label;
        }
        else
        {
            return "tile.computercraft:printer.name";
        }
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName()
    {
        if( hasCustomName() )
        {
            return new TextComponentString( getName() );
        }
        else
        {
            return new TextComponentTranslation( getName() );
        }
    }

    @Override
    public boolean isUsableByPlayer( @Nonnull EntityPlayer player )
    {
        return isUsable( player, false );
    }

    // ISidedInventory implementation

    @Nonnull
    @Override
    public int[] getSlotsForFace( @Nonnull EnumFacing side )
    {
        switch( side )
        {
            case DOWN:
                return bottomSlots;    // Bottom (Out tray)
            case UP:
                return topSlots; // Top (In tray)
            default:
                return sideSlots;     // Sides (Ink)
        }
    }

    @Override
    public boolean canInsertItem( int slot, @Nonnull ItemStack itemstack, @Nonnull EnumFacing face )
    {
        return isItemValidForSlot( slot, itemstack );
    }

    @Override
    public boolean canExtractItem( int slot, @Nonnull ItemStack itemstack, @Nonnull EnumFacing face )
    {
        return true;
    }

    // IPeripheralTile implementation

    public boolean isBottomFull()
    {
        return bottomFull;
    }

    public boolean isTopFull()
    {
        return topFull;
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return new PrinterPeripheral( this );
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel( String label )
    {
        this.label = label;
    }

    public Terminal getCurrentPage()
    {
        if( m_printing )
        {
            return m_page;
        }
        return null;
    }

    public boolean startNewPage()
    {
        synchronized( m_inventory )
        {
            if( canInputPage() )
            {
                if( m_printing && !outputPage() )
                {
                    return false;
                }
                if( inputPage() )
                {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean endCurrentPage()
    {
        synchronized( m_inventory )
        {
            if( m_printing && outputPage() )
            {
                return true;
            }
        }
        return false;
    }

    public int getInkLevel()
    {
        synchronized( m_inventory )
        {
            ItemStack inkStack = m_inventory.get( 0 );
            if( !inkStack.isEmpty() && isInk( inkStack ) )
            {
                return inkStack.getCount();
            }
        }
        return 0;
    }

    public int getPaperLevel()
    {
        int count = 0;
        synchronized( m_inventory )
        {
            for( int i = 1; i < 7; ++i )
            {
                ItemStack paperStack = m_inventory.get( i );
                if( !paperStack.isEmpty() && isPaper( paperStack ) )
                {
                    count += paperStack.getCount();
                }
            }
        }
        return count;
    }

    public void setPageTitle( String title )
    {
        if( m_printing )
        {
            m_pageTitle = title;
        }
    }

    private boolean isInk( @Nonnull ItemStack stack )
    {
        return (stack.getItem() == Items.DYE);
    }

    private boolean isPaper( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        return item == Items.PAPER || item == ComputerCraft.Items.printedPage;
    }

    private boolean canInputPage()
    {
        synchronized( m_inventory )
        {
            ItemStack inkStack = m_inventory.get( 0 );
            return !inkStack.isEmpty() && isInk( inkStack ) && getPaperLevel() > 0;
        }
    }

    private boolean inputPage()
    {
        synchronized( m_inventory )
        {
            ItemStack inkStack = m_inventory.get( 0 );
            if( inkStack.isEmpty() || !isInk( inkStack ) )
            {
                return false;
            }

            for( int i = 1; i < 7; ++i )
            {
                ItemStack paperStack = m_inventory.get( i );
                if( !paperStack.isEmpty() && isPaper( paperStack ) )
                {
                    // Setup the new page
                    int colour = inkStack.getItemDamage();
                    if( colour >= 0 && colour < 16 )
                    {
                        m_page.setTextColour( 15 - colour );
                    }
                    else
                    {
                        m_page.setTextColour( 15 );
                    }

                    m_page.clear();
                    if( paperStack.getItem() instanceof ItemPrintout )
                    {
                        m_pageTitle = ItemPrintout.getTitle( paperStack );
                        String[] text = ItemPrintout.getText( paperStack );
                        String[] textColour = ItemPrintout.getColours( paperStack );
                        for( int y = 0; y < m_page.getHeight(); ++y )
                        {
                            m_page.setLine( y, text[y], textColour[y], "" );
                        }
                    }
                    else
                    {
                        m_pageTitle = "";
                    }
                    m_page.setCursorPos( 0, 0 );

                    // Decrement ink
                    inkStack.shrink( 1 );
                    if( inkStack.isEmpty() )
                    {
                        m_inventory.set( 0, ItemStack.EMPTY );
                    }

                    // Decrement paper
                    paperStack.shrink( 1 );
                    if( paperStack.isEmpty() )
                    {
                        m_inventory.set( i, ItemStack.EMPTY );
                        updateAnim();
                    }

                    markDirty();
                    m_printing = true;
                    return true;
                }
            }
            return false;
        }
    }

    private boolean outputPage()
    {
        synchronized( m_page )
        {
            int height = m_page.getHeight();
            String[] lines = new String[height];
            String[] colours = new String[height];
            for( int i = 0; i < height; ++i )
            {
                lines[i] = m_page.getLine( i ).toString();
                colours[i] = m_page.getTextColourLine( i ).toString();
            }

            ItemStack stack = ItemPrintout.createSingleFromTitleAndText( m_pageTitle, lines, colours );
            synchronized( m_inventory )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, m_itemHandlerAll, 7, 6, 7 );
                if( remainder.isEmpty() )
                {
                    m_printing = false;
                    return true;
                }
            }
            return false;
        }
    }

    private void ejectContents()
    {
        synchronized( m_inventory )
        {
            for( int i = 0; i < 13; ++i )
            {
                ItemStack stack = m_inventory.get( i );
                if( !stack.isEmpty() )
                {
                    // Remove the stack from the inventory
                    setInventorySlotContents( i, ItemStack.EMPTY );

                    // Spawn the item in the world
                    BlockPos pos = getPos();
                    double x = pos.getX() + 0.5;
                    double y = pos.getY() + 0.75;
                    double z = pos.getZ() + 0.5;
                    EntityItem entityitem = new EntityItem( getWorld(), x, y, z, stack );
                    entityitem.motionX = getWorld().rand.nextFloat() * 0.2 - 0.1;
                    entityitem.motionY = getWorld().rand.nextFloat() * 0.2 - 0.1;
                    entityitem.motionZ = getWorld().rand.nextFloat() * 0.2 - 0.1;
                    getWorld().spawnEntity( entityitem );
                }
            }
        }
    }

    private void updateAnim()
    {
        boolean bottomFull = false, topFull = false;
        synchronized( m_inventory )
        {

            for( int i = 1; i < 7; ++i )
            {
                ItemStack stack = m_inventory.get( i );
                if( !stack.isEmpty() && isPaper( stack ) )
                {
                    bottomFull = true;
                    break;
                }
            }
            for( int i = 7; i < 13; ++i )
            {
                ItemStack stack = m_inventory.get( i );
                if( !stack.isEmpty() && isPaper( stack ) )
                {
                    topFull = true;
                    break;
                }
            }
        }

        if( topFull != this.topFull || bottomFull != this.bottomFull )
        {
            this.topFull = topFull;
            this.bottomFull = bottomFull;
            getWorld().markBlockRangeForRenderUpdate( getPos(), getPos() );
        }
    }

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing facing )
    {
        return capability == ITEM_HANDLER_CAPABILITY || super.hasCapability( capability, facing );
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == ITEM_HANDLER_CAPABILITY )
        {
            if( facing == null )
            {
                return ITEM_HANDLER_CAPABILITY.cast( m_itemHandlerAll );
            }
            else
            {
                IItemHandlerModifiable[] handlers = m_itemHandlerSides;
                if( handlers == null ) handlers = m_itemHandlerSides = new IItemHandlerModifiable[6];

                int i = facing.ordinal();
                IItemHandlerModifiable handler = handlers[i];
                if( handler == null ) handler = handlers[i] = new SidedInvWrapper( this, facing );

                return ITEM_HANDLER_CAPABILITY.cast( handler );
            }
        }
        return super.getCapability( capability, facing );
    }
}
