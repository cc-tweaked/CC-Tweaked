package dan200.computercraft.client.gui;

import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

public abstract class ComputerScreenBase <T extends ContainerComputerBase> extends HandledScreen<T> {

    private static final Text OK = new TranslatableText( "gui.ok" );
    private static final Text CANCEL = new TranslatableText( "gui.cancel" );
    private static final Text OVERWRITE = new TranslatableText( "gui.computercraft.upload.overwrite_button" );

    protected WidgetTerminal terminal;
    protected final ClientComputer computer;
    protected final ComputerFamily family;

    protected final int sidebarYOffset;

    public ComputerScreenBase(T container, PlayerInventory player, Text title, int sidebarYOffset )
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
        client.keyboard.setRepeatEvents( true );

        terminal = addDrawableChild( createTerminal() );
        ComputerSidebar.addButtons( this, computer, this::addDrawableChild, x, y + sidebarYOffset );
        setFocused( terminal );
    }

    @Override
    public final void removed()
    {
        super.removed();
        client.keyboard.setRepeatEvents( false );
    }

    @Override
    public final void handledScreenTick()
    {
        super.handledScreenTick();
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
    public final void render(@Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        drawMouseoverTooltip( stack, mouseX, mouseY );
    }

    @Override
    public final boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }

    @Override
    protected void drawForeground( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }

}
