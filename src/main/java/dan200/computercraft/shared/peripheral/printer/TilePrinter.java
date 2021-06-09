/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DefaultSidedInventory;
import dan200.computercraft.shared.util.ItemStorage;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TilePrinter extends TileGeneric implements DefaultSidedInventory, IPeripheralTile, Nameable, NamedScreenHandlerFactory
{
    static final int SLOTS = 13;
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_PRINTING = "Printing";
    private static final String NBT_PAGE_TITLE = "PageTitle";
    private static final int[] BOTTOM_SLOTS = new int[] {
        7,
        8,
        9,
        10,
        11,
        12,
    };
    private static final int[] TOP_SLOTS = new int[] {
        1,
        2,
        3,
        4,
        5,
        6,
    };
    private static final int[] SIDE_SLOTS = new int[] { 0 };
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize( SLOTS, ItemStack.EMPTY );
    private final ItemStorage itemHandlerAll = ItemStorage.wrap( this );
    private final Terminal page = new Terminal( ItemPrintout.LINE_MAX_LENGTH, ItemPrintout.LINES_PER_PAGE );
    Text customName;
    private String pageTitle = "";
    private boolean printing = false;

    public TilePrinter( BlockEntityType<TilePrinter> type )
    {
        super( type );
    }

    @Override
    public void destroy()
    {
        this.ejectContents();
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( player.isInSneakingPose() )
        {
            return ActionResult.PASS;
        }

        if( !this.getWorld().isClient )
        {
            player.openHandledScreen( this );
        }
        return ActionResult.SUCCESS;
    }

    private void ejectContents()
    {
        for( int i = 0; i < 13; i++ )
        {
            ItemStack stack = this.inventory.get( i );
            if( !stack.isEmpty() )
            {
                // Remove the stack from the inventory
                this.setStack( i, ItemStack.EMPTY );

                // Spawn the item in the world
                WorldUtil.dropItemStack( stack, this.getWorld(),
                    Vec3d.of( this.getPos() )
                        .add( 0.5, 0.75, 0.5 ) );
            }
        }
    }

    private void updateBlockState()
    {
        boolean top = false, bottom = false;
        for( int i = 1; i < 7; i++ )
        {
            ItemStack stack = this.inventory.get( i );
            if( !stack.isEmpty() && isPaper( stack ) )
            {
                top = true;
                break;
            }
        }
        for( int i = 7; i < 13; i++ )
        {
            ItemStack stack = this.inventory.get( i );
            if( !stack.isEmpty() && isPaper( stack ) )
            {
                bottom = true;
                break;
            }
        }

        this.updateBlockState( top, bottom );
    }

    private static boolean isPaper( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        return item == Items.PAPER || (item instanceof ItemPrintout && ((ItemPrintout) item).getType() == ItemPrintout.Type.PAGE);
    }

    private void updateBlockState( boolean top, boolean bottom )
    {
        if( this.removed )
        {
            return;
        }

        BlockState state = this.getCachedState();
        if( state.get( BlockPrinter.TOP ) == top & state.get( BlockPrinter.BOTTOM ) == bottom )
        {
            return;
        }

        this.getWorld().setBlockState( this.getPos(),
            state.with( BlockPrinter.TOP, top )
                .with( BlockPrinter.BOTTOM, bottom ) );
    }

    @Override
    public void fromTag( @Nonnull BlockState state, @Nonnull CompoundTag nbt )
    {
        super.fromTag( state, nbt );

        this.customName = nbt.contains( NBT_NAME ) ? Text.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;

        // Read page
        synchronized( this.page )
        {
            this.printing = nbt.getBoolean( NBT_PRINTING );
            this.pageTitle = nbt.getString( NBT_PAGE_TITLE );
            this.page.readFromNBT( nbt );
        }

        // Read inventory
        Inventories.fromTag( nbt, this.inventory );
    }

    @Nonnull
    @Override
    public CompoundTag toTag( @Nonnull CompoundTag nbt )
    {
        if( this.customName != null )
        {
            nbt.putString( NBT_NAME, Text.Serializer.toJson( this.customName ) );
        }

        // Write page
        synchronized( this.page )
        {
            nbt.putBoolean( NBT_PRINTING, this.printing );
            nbt.putString( NBT_PAGE_TITLE, this.pageTitle );
            this.page.writeToNBT( nbt );
        }

        // Write inventory
        Inventories.toTag( nbt, this.inventory );

        return super.toTag( nbt );
    }

    boolean isPrinting()
    {
        return this.printing;
    }

    // IInventory implementation
    @Override
    public int size()
    {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : this.inventory )
        {
            if( !stack.isEmpty() )
            {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getStack( int slot )
    {
        return this.inventory.get( slot );
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot, int count )
    {
        ItemStack stack = this.inventory.get( slot );
        if( stack.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        if( stack.getCount() <= count )
        {
            this.setStack( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.split( count );
        if( this.inventory.get( slot )
            .isEmpty() )
        {
            this.inventory.set( slot, ItemStack.EMPTY );
            this.updateBlockState();
        }
        this.markDirty();
        return part;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot )
    {
        ItemStack result = this.inventory.get( slot );
        this.inventory.set( slot, ItemStack.EMPTY );
        this.markDirty();
        this.updateBlockState();
        return result;
    }

    // ISidedInventory implementation

    @Override
    public void setStack( int slot, @Nonnull ItemStack stack )
    {
        this.inventory.set( slot, stack );
        this.markDirty();
        this.updateBlockState();
    }

    @Override
    public boolean canPlayerUse( @Nonnull PlayerEntity playerEntity )
    {
        return this.isUsable( playerEntity, false );
    }

    @Override
    public void clear()
    {
        for( int i = 0; i < this.inventory.size(); i++ )
        {
            this.inventory.set( i, ItemStack.EMPTY );
        }
        this.markDirty();
        this.updateBlockState();
    }

    @Override
    public boolean isValid( int slot, @Nonnull ItemStack stack )
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

    static boolean isInk( @Nonnull ItemStack stack )
    {
        return ColourUtils.getStackColour( stack ) != null;
    }

    @Nonnull
    @Override
    public int[] getAvailableSlots( @Nonnull Direction side )
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

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        return new PrinterPeripheral( this );
    }

    @Nullable
    Terminal getCurrentPage()
    {
        synchronized( this.page )
        {
            return this.printing ? this.page : null;
        }
    }

    boolean startNewPage()
    {
        synchronized( this.page )
        {
            if( !this.canInputPage() )
            {
                return false;
            }
            if( this.printing && !this.outputPage() )
            {
                return false;
            }
            return this.inputPage();
        }
    }

    boolean endCurrentPage()
    {
        synchronized( this.page )
        {
            return this.printing && this.outputPage();
        }
    }

    private boolean outputPage()
    {
        int height = this.page.getHeight();
        String[] lines = new String[height];
        String[] colours = new String[height];
        for( int i = 0; i < height; i++ )
        {
            lines[i] = this.page.getLine( i )
                .toString();
            colours[i] = this.page.getTextColourLine( i )
                .toString();
        }

        ItemStack stack = ItemPrintout.createSingleFromTitleAndText( this.pageTitle, lines, colours );
        for( int slot : BOTTOM_SLOTS )
        {
            if( this.inventory.get( slot )
                .isEmpty() )
            {
                this.setStack( slot, stack );
                this.printing = false;
                return true;
            }
        }
        return false;
    }

    int getInkLevel()
    {
        ItemStack inkStack = this.inventory.get( 0 );
        return isInk( inkStack ) ? inkStack.getCount() : 0;
    }

    int getPaperLevel()
    {
        int count = 0;
        for( int i = 1; i < 7; i++ )
        {
            ItemStack paperStack = this.inventory.get( i );
            if( isPaper( paperStack ) )
            {
                count += paperStack.getCount();
            }
        }
        return count;
    }

    void setPageTitle( String title )
    {
        synchronized( this.page )
        {
            if( this.printing )
            {
                this.pageTitle = title;
            }
        }
    }

    private boolean canInputPage()
    {
        ItemStack inkStack = this.inventory.get( 0 );
        return !inkStack.isEmpty() && isInk( inkStack ) && this.getPaperLevel() > 0;
    }

    private boolean inputPage()
    {
        ItemStack inkStack = this.inventory.get( 0 );
        DyeColor dye = ColourUtils.getStackColour( inkStack );
        if( dye == null ) return false;

        for( int i = 1; i < 7; i++ )
        {
            ItemStack paperStack = this.inventory.get( i );
            if( paperStack.isEmpty() || !isPaper( paperStack ) )
            {
                continue;
            }

            // Setup the new page
            page.setTextColour( dye.getId() );

            this.page.clear();
            if( paperStack.getItem() instanceof ItemPrintout )
            {
                this.pageTitle = ItemPrintout.getTitle( paperStack );
                String[] text = ItemPrintout.getText( paperStack );
                String[] textColour = ItemPrintout.getColours( paperStack );
                for( int y = 0; y < this.page.getHeight(); y++ )
                {
                    this.page.setLine( y, text[y], textColour[y], "" );
                }
            }
            else
            {
                this.pageTitle = "";
            }
            this.page.setCursorPos( 0, 0 );

            // Decrement ink
            inkStack.decrement( 1 );
            if( inkStack.isEmpty() )
            {
                this.inventory.set( 0, ItemStack.EMPTY );
            }

            // Decrement paper
            paperStack.decrement( 1 );
            if( paperStack.isEmpty() )
            {
                this.inventory.set( i, ItemStack.EMPTY );
                this.updateBlockState();
            }

            this.markDirty();
            this.printing = true;
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public Text getName()
    {
        return this.customName != null ? this.customName : new TranslatableText( this.getCachedState().getBlock()
            .getTranslationKey() );
    }

    @Override
    public boolean hasCustomName()
    {
        return this.customName != null;
    }

    @Override
    public Text getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Nullable
    @Override
    public Text getCustomName()
    {
        return this.customName;
    }

    @Nonnull
    @Override
    public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerPrinter( id, inventory, this );
    }
}
