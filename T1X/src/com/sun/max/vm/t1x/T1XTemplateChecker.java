/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Tests the IR for a T1X template to ensure it does not write to a Java stack or
 * local slot before a <i>stop</i> instruction (i.e. any instruction to which a GC
 * map is attached).
 * <p>
 * This invariant simplifies root scanning of a frame compiled by T1X.
 * A GC reference map derived for the bytecode state at the start of a
 * template will be valid for all stops in the template.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public class T1XTemplateChecker extends C1XCompilerExtension {

    IR ir;
    boolean changed;
    IdentityHashMap<BlockBegin, BlockBegin> map = new IdentityHashMap<BlockBegin, BlockBegin>();

    boolean seenFrameModification(BlockBegin block) {
        return map.containsKey(block);
    }

    void setSeenFrameModification(BlockBegin block) {
        map.put(block, block);
    }

    class Helper extends DefaultValueVisitor implements BlockClosure {

        BlockBegin block;
        boolean seenFrameModification;

        @Override
        public void apply(BlockBegin block) {
            this.block = block;
            seenFrameModification = seenFrameModification(block);
            Instruction i = block;
            while ((i = i.next()) != null) {
                if (i.stateBefore() != null) {
                    if (seenFrameModification) {
                        FatalError.unexpected("Java bytecode frame updated before a safepoint or call:\n" + i.stateBefore().toCodePos());
                    }
                }

                i.accept(this);
            }

            if (seenFrameModification) {
                for (BlockBegin succ : block.end().successors()) {
                    boolean succSeenFrameModification = seenFrameModification(succ);
                    if (!succSeenFrameModification) {
                        setSeenFrameModification(succ);
                        changed = true;
                    }
                }
            }
        }

        @Override
        public void visitStorePointer(StorePointer i) {
            if (i.pointer() instanceof LoadRegister) {
                CiRegister reg = ((LoadRegister) i.pointer()).register;
                if (reg == MaxineVM.vm().registerConfigs.jitTemplate.getRegisterForRole(VMRegister.ABI_FP) ||
                    reg == MaxineVM.vm().registerConfigs.jitTemplate.getRegisterForRole(VMRegister.ABI_SP) ||
                    reg == MaxineVM.vm().registerConfigs.jitTemplate.getRegisterForRole(VMRegister.CPU_FP) ||
                    reg == MaxineVM.vm().registerConfigs.jitTemplate.getRegisterForRole(VMRegister.CPU_SP)) {
                    setSeenFrameModification(block);
                    seenFrameModification = true;
                }
            }
        }
    }

    @Override
    protected void process(IR ir) {
        this.ir = ir;
        FatalError.check(ir.compilation.method.exceptionHandlers().length == 0, "T1X template cannot have exception handlers: " + ir.compilation.method);
        Helper helper = new Helper();
        int iterations = 0;
        do {
            changed = false;
            ir.startBlock.iterateAnyOrder(helper, false);
            iterations++;
        } while (changed);
    }
}
