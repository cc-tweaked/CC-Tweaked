/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.DynamicImageButton;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.server.ContinueUploadMessage;
import dan200.computercraft.shared.network.server.UploadFileMessage;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ComputerScreenBase<T extends ContainerComputerBase> extends ContainerScreen<T>
{
    private static final ITextComponent OK = new TranslationTextComponent( "gui.ok" );
    private static final ITextComponent CANCEL = new TranslationTextComponent( "gui.cancel" );
    private static final ITextComponent OVERWRITE = new TranslationTextComponent( "gui.computercraft.upload.overwrite_button" );

    protected WidgetTerminal terminal;
    protected final ClientComputer computer;
    protected final ComputerFamily family;

    protected final int sidebarYOffset;

    public ComputerScreenBase( T container, PlayerInventory player, ITextComponent title, int sidebarYOffset )
    {
        super( container, player, title );
        computer = (ClientComputer) container.getComputer();
        family = container.getFamily();
        this.sidebarYOffset = sidebarYOffset;
    }

    protected abstract WidgetTerminal createTerminal();

    @Override
    protected final void init()
    {
        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        terminal = addButton( createTerminal() );
        ComputerSidebar.addButtons( this, computer, this::addButton, leftPos, topPos + sidebarYOffset );
        setFocused( terminal );
    }

    @Override
    public final void removed()
    {
        super.removed();
        minecraft.keyboardHandler.setSendRepeatsToGui( false );
    }

    @Override
    public final void tick()
    {
        super.tick();
        terminal.update();
    }

    @Override
    public final boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }


    @Override
    public final void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        renderTooltip( stack, mouseX, mouseY );
    }

    @Override
    public boolean mouseClicked( double x, double y, int button )
    {
        boolean changed = super.mouseClicked( x, y, button );
        // Clicking the terminate/shutdown button steals focus, which means then pressing "enter" will click the button
        // again. Restore the focus to the terminal in these cases.
        if( getFocused() instanceof DynamicImageButton ) setFocused( terminal );
        return changed;
    }

    @Override
    public final boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }


    @Override
    protected void renderLabels( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }

    @Override
    public void onFilesDrop( @Nonnull List<Path> files )
    {
        if( files.isEmpty() ) return;

        if( computer == null || !computer.isOn() )
        {
            alert( UploadResult.FAILED_TITLE, UploadResult.COMPUTER_OFF_MSG );
            return;
        }

        long size = 0;

        List<FileUpload> toUpload = new ArrayList<>();
        for( Path file : files )
        {
            // TODO: Recurse directories? If so, we probably want to shunt this off-thread.
            if( !Files.isRegularFile( file ) ) continue;

            try( SeekableByteChannel sbc = Files.newByteChannel( file ) )
            {
                long fileSize = sbc.size();
                if( fileSize > UploadFileMessage.MAX_SIZE || (size += fileSize) >= UploadFileMessage.MAX_SIZE )
                {
                    alert( UploadResult.FAILED_TITLE, UploadResult.TOO_MUCH_MSG );
                    return;
                }

                String name = file.getFileName().toString();
                if( name.length() > UploadFileMessage.MAX_FILE_NAME )
                {
                    alert( UploadResult.FAILED_TITLE, new TranslationTextComponent( "gui.computercraft.upload.failed.name_too_long" ) );
                    return;
                }

                ByteBuffer buffer = ByteBuffer.allocateDirect( (int) fileSize );
                sbc.read( buffer );
                buffer.flip();

                byte[] digest = FileUpload.getDigest( buffer );
                if( digest == null )
                {
                    alert( UploadResult.FAILED_TITLE, new TranslationTextComponent( "gui.computercraft.upload.failed.corrupted" ) );
                    return;
                }

                toUpload.add( new FileUpload( name, buffer, digest ) );
            }
            catch( IOException e )
            {
                ComputerCraft.log.error( "Failed uploading files", e );
                alert( UploadResult.FAILED_TITLE, new TranslationTextComponent( "gui.computercraft.upload.failed.generic", "Cannot compute checksum" ) );
            }
        }

        if( toUpload.size() > UploadFileMessage.MAX_FILES )
        {
            alert( UploadResult.FAILED_TITLE, new TranslationTextComponent( "gui.computercraft.upload.failed.too_many_files" ) );
            return;
        }

        if( toUpload.size() > 0 )
        {
            UploadFileMessage.send( computer.getInstanceID(), toUpload );
        }
    }

    public void uploadResult( UploadResult result, ITextComponent message )
    {
        switch( result )
        {
            case SUCCESS:
                alert( UploadResult.SUCCESS_TITLE, message );
                break;
            case ERROR:
                alert( UploadResult.FAILED_TITLE, message );
                break;
            case CONFIRM_OVERWRITE:
                OptionScreen.show(
                    minecraft, UploadResult.UPLOAD_OVERWRITE, message,
                    Arrays.asList(
                        OptionScreen.newButton( CANCEL, b -> cancelUpload() ),
                        OptionScreen.newButton( OVERWRITE, b -> continueUpload() )
                    ),
                    this::cancelUpload
                );
                break;
        }
    }

    private void continueUpload()
    {
        if( minecraft.screen instanceof OptionScreen ) ((OptionScreen) minecraft.screen).disable();
        NetworkHandler.sendToServer( new ContinueUploadMessage( computer.getInstanceID(), true ) );
    }

    private void cancelUpload()
    {
        minecraft.setScreen( this );
        NetworkHandler.sendToServer( new ContinueUploadMessage( computer.getInstanceID(), false ) );
    }

    private void alert( ITextComponent title, ITextComponent message )
    {
        OptionScreen.show( minecraft, title, message,
            Collections.singletonList( OptionScreen.newButton( OK, b -> minecraft.setScreen( this ) ) ),
            () -> minecraft.setScreen( this )
        );
    }
}
