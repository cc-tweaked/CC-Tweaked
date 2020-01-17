/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.Set;

public class GuiConfigCC extends GuiConfig
{
    public GuiConfigCC( GuiScreen parentScreen )
    {
        super( parentScreen, Config.getConfigElements(), ComputerCraft.MOD_ID, false, false, "CC: Tweaked" );
    }

    public static class Factory implements IModGuiFactory
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
