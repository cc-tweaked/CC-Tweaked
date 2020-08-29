package dan200.computercraft.client;

import java.util.function.Function;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.GuiConfig;
import io.github.prospector.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screen.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public String getModId() {
        return ComputerCraft.MOD_ID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return GuiConfig::getScreen;
    }
}
