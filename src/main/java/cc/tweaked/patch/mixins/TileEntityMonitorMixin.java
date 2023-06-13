// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package cc.tweaked.patch.mixins;

import cc.tweaked.patch.framework.transform.MergeVisitor;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import dan200.computer.core.Terminal;
import dan200.computer.shared.NetworkedTerminalHelper;
import dan200.computer.shared.TileEntityMonitor;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.core.asm.LuaMethod;
import dan200.computercraft.core.asm.Methods;
import dan200.computercraft.core.asm.NamedMethod;
import dan200.computercraft.shared.peripheral.monitor.MonitorPeripheral;
import dan200.computercraft.shared.peripheral.monitor.TileEntityMonitorAccessor;

import java.util.List;

/**
 * Replaces the {@link TileEntityMonitor} peripheral with {@link MonitorPeripheral}.
 */
public abstract class TileEntityMonitorMixin implements TileEntityMonitorAccessor, IPeripheral {
    private @MergeVisitor.Shadow int m_textScale;
    private @MergeVisitor.Shadow NetworkedTerminalHelper m_terminal;
    private @MergeVisitor.Shadow boolean m_changed;

    private List<NamedMethod<LuaMethod>> methods;
    private MonitorPeripheral peripheral;

    public String[] getMethodNames() {
        // Overwrite the original getMethodNames, instead deferring to our peripheral.
        if (methods == null) {
            peripheral = new MonitorPeripheral(this);
            methods = Methods.LUA_METHOD.getMethods(peripheral.getClass());
        }

        String[] names = new String[methods.size()];
        for (int i = 0; i < methods.size(); i++) names[i] = methods.get(i).getName();
        return names;
    }

    public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws Exception {
        return methods.get(method).getMethod().apply(peripheral, new ObjectArguments(arguments));
    }

    @Override
    public Terminal cct$getOriginTerminal() {
        return origin().getTerminal();
    }

    @Override
    public void cct$setTextScale(int scale) {
        TileEntityMonitorMixin origin = origin();
        synchronized (origin.m_terminal) {
            if (origin.m_textScale != scale) {
                origin.m_textScale = scale;
                origin.rebuildTerminal(null);
                origin.m_changed = true;
            }
        }
    }

    @Override
    public int cct$getTextScale() {
        return origin().m_textScale;
    }

    @MergeVisitor.Shadow
    private TileEntityMonitorMixin origin() {
        throw new AssertionError("Stub method");
    }

    @MergeVisitor.Shadow
    public abstract Terminal getTerminal();

    @MergeVisitor.Shadow
    private void rebuildTerminal(Terminal copyFrom) {
        throw new AssertionError("Stub method");
    }
}
