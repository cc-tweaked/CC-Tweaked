// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.relauncher.FMLRelaunchLog;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

@Mod(
    modid = "cctweaked", name = "CC: Tweaked",
    version = "1.105.0",
    dependencies = "required-after:ComputerCraft"
)
public class CCTweaked {
    public static final Logger LOG = Logger.getLogger("CC: Tweaked");

    static {
        LOG.setParent(FMLRelaunchLog.log.getLogger());
    }

    public static File getLoadingJar() {
        String path = CCTweaked.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int bangIndex = path.indexOf('!');
        if (bangIndex >= 0) path = path.substring(0, bangIndex);

        URL url;
        try {
            url = new URL(path);
        } catch (MalformedURLException e1) {
            return null;
        }

        File file;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            file = new File(url.getPath());
        }
        return file;
    }
}
