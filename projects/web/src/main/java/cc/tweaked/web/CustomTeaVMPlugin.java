// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import com.google.auto.service.AutoService;
import org.teavm.model.*;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.List;

/**
 * Custom {@link TeaVMPlugin} to apply additional class transformations.
 */
@AutoService(TeaVMPlugin.class)
public class CustomTeaVMPlugin implements TeaVMPlugin {
    private static final String LUA_STATE = "org.squiddev.cobalt.LuaState";
    private static final String COBALT_MACHINE = "dan200.computercraft.core.lua.CobaltLuaMachine";

    @Override
    public void install(TeaVMHost host) {
        host.add(CustomTeaVMPlugin::patchCobalt);
    }

    /**
     * Patch Cobalt's {@code LuaState} and CC's {@code CobaltLuaMachine} to self-interrupt.
     * <p>
     * In normal CC:T, computers are paused/interrupted asynchronously from the {@code ComputerThread}. However, as
     * Javascript doesn't (easily) support multi-threaded code, we must find another option.
     * <p>
     * Instead, we patch {@code LuaState.isInterrupted()} to periodically return true (every 1024 instructions), and then
     * patch the interruption callback to refresh the timeout state. This means that the machine runs in very small time
     * slices which, while quite slow, ensures we never block the UI thread for very long.
     *
     * @param cls     The class to patch.
     * @param context The class transformation context.
     */
    private static void patchCobalt(ClassHolder cls, ClassHolderTransformerContext context) {
        switch (cls.getName()) {
            case LUA_STATE -> transformLuaState(cls, context.getHierarchy());
            case COBALT_MACHINE -> transformCobaltMachine(cls);
        }
    }

    /**
     * Patch Cobalt's {@code LuaState.isInterrupted()} to periodically return true.
     *
     * @param cls       The class to patch.
     * @param hierarchy The currently loaded class hierarchy.
     */
    private static void transformLuaState(ClassHolder cls, ClassHierarchy hierarchy) {
        var countField = new FieldHolder("$count");
        countField.setType(ValueType.INTEGER);
        cls.addField(countField);

        var isInterrupted = cls.getMethod(new MethodDescriptor("isInterrupted", ValueType.BOOLEAN));
        var code = ProgramEmitter.create(isInterrupted.getDescriptor(), hierarchy);

        // int x = ($count + 1) & 1023;
        var incremented = code.var(0, cls).getField("$count", ValueType.INTEGER).add(1).bitAnd(1023);
        // $count = x;
        code.var(0, cls).setField("$count", incremented);
        // return $x == 0
        code.when(incremented.isEqualTo(code.constant(0)))
            .thenDo(() -> code.constant(1).returnValue())
            .elseDo(() -> code.constant(0).returnValue());

        isInterrupted.setProgram(code.getProgram());
    }

    /**
     * Patch {@code CobaltLuaMachine} to call {@code TimeoutState.refresh()} at the head of the function.
     *
     * @param cls The class instance to patch.
     */
    private static void transformCobaltMachine(ClassHolder cls) {
        var method = cls.getMethods().stream().filter(x ->
            x.getName().startsWith("lambda$")
                && x.getDescriptor().parameterCount() == 0
                && x.getDescriptor().getResultType().isObject("org.squiddev.cobalt.interrupt.InterruptAction")
        ).findFirst().orElseThrow();

        var program = method.getProgram();
        var bb = program.basicBlockAt(0);

        var getTimeoutVar = program.createVariable();
        var getTimeout = new GetFieldInstruction();
        getTimeout.setField(new FieldReference(COBALT_MACHINE, "timeout"));
        getTimeout.setInstance(method.getProgram().variableAt(0));
        getTimeout.setReceiver(getTimeoutVar);
        getTimeout.setFieldType(ValueType.object("dan200.computercraft.core.computer.TimeoutState"));

        var refresh = new InvokeInstruction();
        refresh.setType(InvocationType.VIRTUAL);
        refresh.setInstance(getTimeoutVar);
        refresh.setMethod(new MethodReference("dan200.computercraft.core.computer.TimeoutState", "refresh", ValueType.VOID));

        bb.addFirstAll(List.of(getTimeout, refresh));
    }
}
