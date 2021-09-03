/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class NoTermComputerScreen<T extends ContainerComputerBase> extends Screen implements IHasContainer<T>
{
    @Nullable
    private static final Field mineScreen = TryFindEncodedField(); //try find minecraft.screen field

    private final T menu;
    private WidgetTerminal terminal;

    public NoTermComputerScreen( T menu, PlayerInventory player, ITextComponent title )
    {
        super( title );
        this.menu = menu;
    }

    @Nonnull
    @Override
    public T getMenu()
    {
        return menu;
    }

    @Override
    protected void init()
    {
        this.passEvents = true; // to allow gui click events pass through mouseHelper protection (see MouseHelper.OnPres:105 code string)
        if (mineScreen != null) // if reflection failed - pocket pc still would work but with no mouse move mechanic
        {
            minecraft.mouseHandler.grabMouse();
            try {
                mineScreen.set(minecraft, this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        terminal = addWidget( new WidgetTerminal( (ClientComputer) menu.getComputer(), 0, 0, ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight ) );
        terminal.visible = false;
        terminal.active = false;
        setFocused( terminal );
    }

    // There is only one field in Minecraft that has net.minecraft.client.gui.screen.Screen type: it is field_71462_r (for 1.16.5)
    // I am not sure that using this name could be safe, so I search for screen using it's Type
    private static Field TryFindEncodedField()
    {
        final String success = "Screen found. Pocket pc mouse move enabled.";
        try { // trying to find screen by in_game name (faster means better)
            Field field = Minecraft.class.getDeclaredField("field_71462_r");
            if (field.getType() == Screen.class) {
                field.setAccessible(true);
                ComputerCraft.log.info(success);
                return field;
            }
        } catch (NoSuchFieldException ignored){} //there is nothing to wright or do if Exception caught, so...
        ComputerCraft.log.info("Fast screen finding failed. Try find by type.");

        Field[] fields = Minecraft.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == Screen.class) {
                field.setAccessible(true);
                ComputerCraft.log.info(success);
                return field;
            }
        }
        ComputerCraft.log.info("Unable to found screen. Pocket pc mouse move disabled");
        return null;
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
    public boolean mouseScrolled(double p_231043_1_, double p_231043_3_, double p_231043_5_) {
        minecraft.player.inventory.swapPaint(p_231043_5_);
        return super.mouseScrolled(p_231043_1_, p_231043_3_, p_231043_5_);
    }

    @Override
    public void onClose()
    {
        minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
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
    public void render( MatrixStack transform, int mouseX, int mouseY, float partialTicks )
    {
        super.render( transform, mouseX, mouseY, partialTicks );

        FontRenderer font = minecraft.font;
        List<IReorderingProcessor> lines = font.split( new TranslationTextComponent( "gui.computercraft.pocket_computer_overlay" ), (int) (width * 0.8) );
        float y = 10.0f;
        for( IReorderingProcessor line : lines )
        {
            font.drawShadow( transform, line, (float) ((width / 2) - (minecraft.font.width( line ) / 2)), y, 0xFFFFFF );
            y += 9.0f;
        }
    }
}

