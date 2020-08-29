/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.shared.common.ClientTerminal;

import net.minecraft.util.math.BlockPos;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ClientMonitor extends ClientTerminal {
    private static final Set<ClientMonitor> allMonitors = new HashSet<>();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public BlockPos lastRenderPos = null;
    public int[] renderDisplayLists = null;

    public ClientMonitor(boolean colour, TileMonitor origin) {
        super(colour);
        this.origin = origin;
    }

    @Environment (EnvType.CLIENT)
    public static void destroyAll() {
        synchronized (allMonitors) {
            for (Iterator<ClientMonitor> iterator = allMonitors.iterator(); iterator.hasNext(); ) {
                ClientMonitor monitor = iterator.next();
                if (monitor.renderDisplayLists != null) {
                    for (int list : monitor.renderDisplayLists) {
                        GlStateManager.deleteLists(list, 1);
                    }
                    monitor.renderDisplayLists = null;
                }

                iterator.remove();
            }
        }
    }

    public TileMonitor getOrigin() {
        return this.origin;
    }

    @Environment (EnvType.CLIENT)
    public void createLists() {
        if (this.renderDisplayLists == null) {
            this.renderDisplayLists = new int[3];

            for (int i = 0; i < this.renderDisplayLists.length; i++) {
                this.renderDisplayLists[i] = GlStateManager.genLists(1);
            }

            synchronized (allMonitors) {
                allMonitors.add(this);
            }
        }
    }

    @Environment (EnvType.CLIENT)
    public void destroy() {
        if (this.renderDisplayLists != null) {
            synchronized (allMonitors) {
                allMonitors.remove(this);
            }

            for (int list : this.renderDisplayLists) {
                GlStateManager.deleteLists(list, 1);
            }

            this.renderDisplayLists = null;
        }
    }
}
