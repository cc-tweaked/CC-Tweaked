// SPDX-FileCopyrightText: 2009-2011 Luaj.org, 2015-2020 SquidDev
//
// SPDX-License-Identifier: MIT

package dan200.computercraft.core.lua.errorinfo;

import org.squiddev.cobalt.Prototype;

import static org.squiddev.cobalt.Lua.*;

/**
 * Extracted parts of Cobalt's {@link org.squiddev.cobalt.debug.DebugHelpers}.
 */
final class DebugHelpers {
    private DebugHelpers() {
    }

    private static int filterPc(int pc, int jumpTarget) {
        return pc < jumpTarget ? -1 : pc;
    }

    /**
     * Find the PC where a register was last set.
     * <p>
     * This makes some assumptions about the structure of the bytecode, namely that there are no back edges within the
     * CFG. As a result, this is only valid for temporary values, and not locals.
     *
     * @param pt     The function prototype.
     * @param lastPc The PC to work back from.
     * @param reg    The register.
     * @return The last instruction where the register was set, or {@code -1} if not defined.
     */
    static int findSetReg(Prototype pt, int lastPc, int reg) {
        var lastInsn = -1; // Last instruction that changed "reg";
        var jumpTarget = 0; // Any code before this address is conditional

        for (var pc = 0; pc < lastPc; pc++) {
            var i = pt.code[pc];
            var op = GET_OPCODE(i);
            var a = GETARG_A(i);
            switch (op) {
                case OP_LOADNIL -> {
                    var b = GETARG_B(i);
                    if (a <= reg && reg <= a + b) lastInsn = filterPc(pc, jumpTarget);
                }
                case OP_TFORCALL -> {
                    if (a >= a + 2) lastInsn = filterPc(pc, jumpTarget);
                }
                case OP_CALL, OP_TAILCALL -> {
                    if (reg >= a) lastInsn = filterPc(pc, jumpTarget);
                }
                case OP_JMP -> {
                    var dest = pc + 1 + GETARG_sBx(i);
                    // If jump is forward and doesn't skip lastPc, update jump target
                    if (pc < dest && dest <= lastPc && dest > jumpTarget) jumpTarget = dest;
                }
                default -> {
                    if (testAMode(op) && reg == a) lastInsn = filterPc(pc, jumpTarget);
                }
            }
        }
        return lastInsn;
    }
}
