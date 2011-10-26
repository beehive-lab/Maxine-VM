/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.graal.compiler.lir;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.gen.*;
import com.oracle.max.graal.compiler.util.*;

/**
 * The {@code LIRAssembler} class definition.
 */
public abstract class LIRAssembler {

    public final GraalCompilation compilation;
    public final TargetMethodAssembler tasm;
    public final AbstractAssembler asm;
    public final FrameMap frameMap;
    public int registerRestoreEpilogueOffset = -1;

    protected final List<SlowPath> xirSlowPath;

    private int lastDecodeStart;

    public interface SlowPath {
        void emitCode(LIRAssembler lasm);
    }

    public LIRAssembler(GraalCompilation compilation, TargetMethodAssembler tasm) {
        this.compilation = compilation;
        this.tasm = tasm;
        this.asm = tasm.asm;
        this.frameMap = compilation.frameMap();
        this.xirSlowPath = new ArrayList<SlowPath>();
    }

    public void addSlowPath(SlowPath sp) {
        xirSlowPath.add(sp);
    }

    public void emitLocalStubs() {
        for (SlowPath sp : xirSlowPath) {
            sp.emitCode(this);
        }
        // No more code may be emitted after this point
    }

    public void emitCode(List<LIRBlock> hir) {
        if (GraalOptions.PrintLIR && !TTY.isSuppressed()) {
            LIRList.printLIR(hir);
        }

        for (LIRBlock b : hir) {
            emitBlock(b);
        }
    }

    private void emitBlock(LIRBlock block) {
        if (block.align()) {
            emitAlignment();
        }

        block.setBlockEntryPco(asm.codeBuffer.position());

        if (GraalOptions.PrintLIRWithAssembly) {
            block.printWithoutPhis(TTY.out());
        }

        assert block.lir() != null : "must have LIR";
        if (GraalOptions.CommentedAssembly) {
            String st = String.format(" block B%d", block.blockID());
            tasm.blockComment(st);
        }

        emitLirList(block.lir());
    }

    private void emitLirList(LIRList list) {
        for (LIRInstruction op : list.instructionsList()) {
            if (GraalOptions.CommentedAssembly) {
                // Only print out branches
                if (op.code instanceof LIRBranch) {
                    tasm.blockComment(op.toStringWithIdPrefix());
                }
            }
            if (GraalOptions.PrintLIRWithAssembly && !TTY.isSuppressed()) {
                // print out the LIR operation followed by the resulting assembly
                TTY.println(op.toStringWithIdPrefix());
                TTY.println();
            }

            emitOp(op);

            if (GraalOptions.PrintLIRWithAssembly) {
                printAssembly(asm);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void emitOp(LIRInstruction op) {
        op.code.emitCode(this, op);
    }

    private void printAssembly(AbstractAssembler asm) {
        byte[] currentBytes = asm.codeBuffer.copyData(lastDecodeStart, asm.codeBuffer.position());
        if (currentBytes.length > 0) {
            String disasm = compilation.compiler.runtime.disassemble(currentBytes, lastDecodeStart);
            if (disasm.length() != 0) {
                TTY.println(disasm);
            } else {
                TTY.println("Code [+%d]: %d bytes", lastDecodeStart, currentBytes.length);
                Util.printBytes(lastDecodeStart, currentBytes, GraalOptions.PrintAssemblyBytesPerLine);
            }
        }
        lastDecodeStart = asm.codeBuffer.position();
    }



    public abstract void emitTraps();

    public abstract void emitDeoptizationStub(LIRGenerator.DeoptimizationStub stub);

    protected abstract void emitAlignment();
}
