// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch;

import cc.tweaked.patch.framework.TransformationChain;
import cc.tweaked.patch.framework.transform.BasicRemapper;
import cc.tweaked.patch.framework.transform.ClassMerger;
import cc.tweaked.patch.framework.transform.ReplaceConstant;
import cc.tweaked.patch.framework.transform.Transform;
import cpw.mods.fml.relauncher.IClassTransformer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;

public class ClassTransformer implements IClassTransformer {
    private final TransformationChain chain = new TransformationChain()
        // Use Cobalt instead of LuaJ
        .atMethod(
            "dan200.computer.core.Computer", "initLua", "()V",
            BasicRemapper.remapType("dan200/computer/core/LuaJLuaMachine", "dan200/computercraft/core/lua/CobaltLuaMachine").toMethodTransform()
        )
        // Replace a bunch of core APIS
        .atMethod(
            "dan200.computer.core.Computer", "createAPIs", "()V",
            BasicRemapper.builder()
                .remapType("dan200/computer/core/apis/FSAPI", "dan200/computercraft/core/apis/FSAPI")
                .remapType("dan200/computer/core/apis/OSAPI", "dan200/computercraft/core/apis/OSAPI")
                .remapType("dan200/computer/core/apis/TermAPI", "dan200/computercraft/core/apis/TermAPI")
                .build().toMethodTransform()
        )
        // Replace the monitor peripheral
        .atClass("dan200.computer.shared.TileEntityMonitor", new ClassMerger(
            "dan200.computer.shared.TileEntityMonitor",
            "cc.tweaked.patch.mixins.TileEntityMonitorMixin"
        ))
        // Load from our ROM instead of the CC one. We do this by:
        // 1. Changing the path of the assets folder.
        .atMethod(
            "dan200.computer.core.Computer", "initLua", "()V",
            ReplaceConstant.replace("/lua/bios.lua", "/assets/cctweaked/lua/bios.lua")
        )
        .atMethod(
            "dan200.computer.core.FileSystem", "romMount", "(Ljava/lang/String;Ljava/io/File;)V",
            ReplaceConstant.replace("lua/rom/", "assets/cctweaked/lua/rom/")
        )
        // 2. Reading the assets from the CC:T jar instead of the CC one.
        .atMethod("dan200.computer.shared.NetworkedComputerHelper", "getLoadingJar", "()Ljava/io/File;", mv -> new MethodVisitor(Opcodes.ASM4, mv) {
            @Override
            public void visitCode() {
                super.visitCode();

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "cc/tweaked/CCTweaked", "getLoadingJar", "()Ljava/io/File;");
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();

                mv = null;
            }
        })
        // Ensure we render non-advanced terminals as greyscale
        .atClass("dan200.computer.client.GuiTerminal", new RedirectDrawString(mv -> {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "dan200/computer/client/GuiTerminal", "m_terminal", "Ldan200/computer/shared/IComputerEntity;");
        }))
        .atClass("dan200.computer.client.TileEntityMonitorRenderer", new RedirectDrawString(mv -> {
            mv.visitVarInsn(ALOAD, 1);
        }));

    {
        Transform<ClassVisitor> fwfr = BasicRemapper.remapType("dan200/computer/client/FixedWidthFontRenderer", "dan200/computercraft/client/FixedWidthFontRenderer").toClassTransform();
        for (String klass : new String[]{
            "dan200.computer.client.ComputerCraftProxyClient",
            "dan200.computer.client.GuiPrintout",
            "dan200.computer.client.GuiTerminal",
            "dan200.computer.client.TileEntityMonitorRenderer",
            "dan200.computer.server.ComputerCraftProxyServer",
            "dan200.computer.shared.ComputerCraftProxyCommon",
            "dan200.computer.shared.IComputerCraftProxy",
            "dan200.ComputerCraft",
        }) {
            chain.atClass(klass, fwfr);
        }
    }

    @Override
    public byte[] transform(String name, byte[] contents) {
        return name.startsWith("dan200") ? chain.transform(name, contents) : contents;
    }
}
