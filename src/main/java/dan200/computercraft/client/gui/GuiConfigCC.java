package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GuiConfigCC extends GuiConfig
{
    public GuiConfigCC( GuiScreen parentScreen )
    {
        super( parentScreen, getConfigElements(), ComputerCraft.MOD_ID, false, false, "CC: Tweaked" );
    }

    private static List<IConfigElement> getConfigElements()
    {
        ArrayList<IConfigElement> elements = new ArrayList<>();
        for( Property property : ComputerCraft.Config.config.getCategory( Configuration.CATEGORY_GENERAL ).getOrderedValues() )
        {
            elements.add( new ConfigElement( property ) );
        }
        return elements;
    }

    public static class Factory
        implements IModGuiFactory
    {

        @Override
        public void initialize( Minecraft minecraft )
        {
        }

        @Override
        public boolean hasConfigGui()
        {
            return true;
        }

        @Override
        public GuiScreen createConfigGui( GuiScreen parentScreen )
        {
            return new GuiConfigCC( parentScreen );
        }

        @Override
        public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
        {
            return null;
        }
    }
}
