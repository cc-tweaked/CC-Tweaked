// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua.errorinfo;

import com.google.common.annotations.VisibleForTesting;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;

import javax.annotation.Nullable;
import java.util.Objects;

import static org.squiddev.cobalt.Lua.*;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_ANY_HOOK;

/**
 * Provides additional info about an error.
 * <p>
 * This is currently an internal and deeply unstable module. It's not clear if doing this via bytecode (rather than an
 * AST) is the correct approach and/or, what the correct design is.
 */
public class ErrorInfoLib {
    private static final int MAX_DEPTH = 8;

    private static final RegisteredFunction[] functions = new RegisteredFunction[]{
        RegisteredFunction.ofV("info_for_nil", ErrorInfoLib::getInfoForNil),
    };

    public static void add(LuaState state) throws LuaError {
        state.registry().getSubTable(Constants.LOADED).rawset("cc.internal.error_info", RegisteredFunction.bind(functions));
    }

    private static Varargs getInfoForNil(LuaState state, Varargs args) throws LuaError {
        var thread = args.arg(1).checkThread();
        var level = args.arg(2).checkInteger();

        var context = getInfoForNil(state, thread, level);
        return context == null ? Constants.NIL : ValueFactory.varargsOf(
            ValueFactory.valueOf(context.op()), ValueFactory.valueOf(context.source().isGlobal()),
            context.source().table(), context.source().key()
        );
    }

    /**
     * Get some additional information about an {@code attempt to $OP (a nil value)} error. This often occurs as a
     * result of a misspelled local, global or table index, and so we attempt to detect those cases.
     *
     * @param state  The current Lua state.
     * @param thread The thread which has errored.
     * @param level  The level where the error occurred. We currently expect this to always be 0.
     * @return Some additional information about the error, where available.
     */
    @VisibleForTesting
    static @Nullable NilInfo getInfoForNil(LuaState state, LuaThread thread, int level) {
        var frame = thread.getDebugState().getFrame(level);
        if (frame == null || frame.closure == null || (frame.flags & FLAG_ANY_HOOK) != 0) return null;

        var prototype = frame.closure.getPrototype();
        var pc = frame.pc;
        var insn = prototype.code[pc];

        // Find what operation we're doing that errored.
        return switch (GET_OPCODE(insn)) {
            case OP_CALL, OP_TAILCALL ->
                NilInfo.of("call", resolveValueSource(state, frame, prototype, pc, GETARG_A(insn), 0));
            case OP_GETTABLE, OP_SETTABLE, OP_SELF ->
                NilInfo.of("index", resolveValueSource(state, frame, prototype, pc, GETARG_A(insn), 0));
            default -> null;
        };
    }

    /**
     * Information about an {@code attempt to $OP (a nil value)} error.
     *
     * @param op     The operation we tried to perform.
     * @param source The expression that resulted in a nil value.
     */
    @VisibleForTesting
    record NilInfo(String op, ValueSource source) {
        public static @Nullable NilInfo of(String op, @Nullable ValueSource values) {
            return values == null ? null : new NilInfo(op, values);
        }
    }

    /**
     * A partially-reconstructed Lua expression. This currently only is used for table indexing ({@code table[key]}.
     *
     * @param isGlobal Whether this is a global table access. This is a best-effort guess, and does not distinguish between
     *                 {@code foo} and {@code _ENV.foo}.
     * @param table    The table being indexed.
     * @param key      The key we tried to index.
     */
    @VisibleForTesting
    record ValueSource(boolean isGlobal, LuaValue table, LuaString key) {
    }

    /**
     * Attempt to partially reconstruct a Lua expression from the current debug state.
     *
     * @param state     The current Lua state.
     * @param frame     The current debug frame.
     * @param prototype The current function.
     * @param pc        The current program counter.
     * @param register  The register where this value was stored.
     * @param depth     The current depth. Starts at 0, and aborts once reaching {@link #MAX_DEPTH}.
     * @return The reconstructed expression, or {@code null} if not available.
     */
    @SuppressWarnings("NullTernary")
    private static @Nullable ValueSource resolveValueSource(LuaState state, DebugFrame frame, Prototype prototype, int pc, int register, int depth) {
        if (depth > MAX_DEPTH) return null;
        if (prototype.getLocalName(register + 1, pc) != null) return null;

        // Find where this register was set. If unknown, then abort.
        pc = DebugHelpers.findSetReg(prototype, pc, register);
        if (pc == -1) return null;

        var insn = prototype.code[pc];
        return switch (GET_OPCODE(insn)) {
            case OP_MOVE -> {
                var a = GETARG_A(insn);
                var b = GETARG_B(insn); // move from `b' to `a'
                yield b < a ? resolveValueSource(state, frame, prototype, pc, register, depth + 1) : null; // Resolve 'b' .
            }
            case OP_GETTABUP, OP_GETTABLE, OP_SELF -> {
                var tableIndex = GETARG_B(insn);
                var keyIndex = GETARG_C(insn);
                // We're only interested in expressions of the form "foo.bar". Showing a "did you mean" hint for
                // "foo[i]" isn't very useful!
                if (!ISK(keyIndex)) yield null;

                var key = prototype.constants[INDEXK(keyIndex)];
                if (key.type() != Constants.TSTRING) yield null;

                var table = GET_OPCODE(insn) == OP_GETTABUP
                    ? frame.closure.getUpvalue(tableIndex).getValue()
                    : evaluate(state, frame, prototype, pc, tableIndex, depth);
                if (table == null) yield null;

                var isGlobal = GET_OPCODE(insn) == OP_GETTABUP && Objects.equals(prototype.getUpvalueName(tableIndex), Constants.ENV);
                yield new ValueSource(isGlobal, table, (LuaString) key);
            }
            default -> null;
        };
    }

    /**
     * Attempt to reconstruct the value of a register.
     *
     * @param state     The current Lua state.
     * @param frame     The current debug frame.
     * @param prototype The current function
     * @param pc        The PC to evaluate at.
     * @param register  The register to evaluate.
     * @param depth     The current depth. Starts at 0, and aborts once reaching {@link #MAX_DEPTH}.
     * @return The reconstructed value, or {@code null} if unavailable.
     */
    @SuppressWarnings("NullTernary")
    private static @Nullable LuaValue evaluate(LuaState state, DebugFrame frame, Prototype prototype, int pc, int register, int depth) {
        if (depth >= MAX_DEPTH) return null;

        // If this is a local, then return its contents.
        if (prototype.getLocalName(register + 1, pc) != null) return frame.stack[register];

        // Otherwise find where this register was set. If unknown, then abort.
        pc = DebugHelpers.findSetReg(prototype, pc, register);
        if (pc == -1) return null;

        var insn = prototype.code[pc];
        var opcode = GET_OPCODE(insn);
        return switch (opcode) {
            case OP_MOVE -> {
                var a = GETARG_A(insn);
                var b = GETARG_B(insn); // move from `b' to `a'
                yield b < a ? evaluate(state, frame, prototype, pc, register, depth + 1) : null; // Resolve 'b'.
            }
            // Load constants
            case OP_LOADK -> prototype.constants[GETARG_Bx(insn)];
            case OP_LOADKX -> prototype.constants[GETARG_Ax(prototype.code[pc + 1])];
            case OP_LOADBOOL -> GETARG_B(insn) == 0 ? Constants.FALSE : Constants.TRUE;
            case OP_LOADNIL -> Constants.NIL;
            // Upvalues and tables.
            case OP_GETUPVAL -> frame.closure.getUpvalue(GETARG_B(insn)).getValue();
            case OP_GETTABLE, OP_GETTABUP -> {
                var table = opcode == OP_GETTABUP
                    ? frame.closure.getUpvalue(GETARG_B(insn)).getValue()
                    : evaluate(state, frame, prototype, pc, GETARG_B(insn), depth + 1);
                if (table == null) yield null;

                var key = evaluateK(state, frame, prototype, pc, GETARG_C(insn), depth + 1);
                yield key == null ? null : safeIndex(state, table, key);
            }
            default -> null;
        };
    }

    private static @Nullable LuaValue evaluateK(LuaState state, DebugFrame frame, Prototype prototype, int pc, int registerOrConstant, int depth) {
        return ISK(registerOrConstant) ? prototype.constants[INDEXK(registerOrConstant)] : evaluate(state, frame, prototype, pc, registerOrConstant, depth + 1);
    }

    private static @Nullable LuaValue safeIndex(LuaState state, LuaValue table, LuaValue key) {
        var loop = 0;
        do {
            LuaValue metatable;
            if (table instanceof LuaTable tbl) {
                var res = tbl.rawget(key);
                if (!res.isNil() || (metatable = tbl.metatag(state, CachedMetamethod.INDEX)).isNil()) return res;
            } else if ((metatable = table.metatag(state, CachedMetamethod.INDEX)).isNil()) {
                return null;
            }

            if (metatable instanceof LuaFunction) return null;

            table = metatable;
        }
        while (++loop < Constants.MAXTAGLOOP);

        return null;
    }
}
