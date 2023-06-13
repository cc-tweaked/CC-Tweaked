// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@IFMLLoadingPlugin.TransformerExclusions({
    "cc.tweaked.patch."
})
public class CorePlugin implements IFMLLoadingPlugin {
    public static final Logger LOG = Logger.getLogger("CC: Tweaked (Core)");

    static {
        LOG.setLevel(Level.ALL);
        LOG.setParent(FMLRelaunchLog.log.getLogger());
    }

    @Override
    public String[] getLibraryRequestClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ "cc.tweaked.patch.ClassTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> map) {
    }
}
