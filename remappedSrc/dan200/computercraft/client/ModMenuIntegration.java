package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.GuiConfig;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

public class ModMenuIntegration implements ModMenuApi
{
    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory()
    {
        return GuiConfig::getScreen;
    }

    @Override
    public String getModId()
    {
        return ComputerCraft.MOD_ID;
    }
}
