/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.c1x;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

public class WordTypeRewriter implements BlockClosure {
    public void apply(C1XCompilation compilation) {
        IR hir = compilation.hir();
        for (int i = hir.startBlock.stateBefore().localsSize() - 1; i >= 0; i--) {
            Local instr = (Local) hir.startBlock.stateBefore().localAt(i);
            if (instr != null) {
                rewriteWord(instr);
            }
        }
        hir.startBlock.iterateAnyOrder(this, false);
    }

    @Override
    public void apply(BlockBegin block) {
        block.stateBefore().forEachPhi(block, new FrameState.PhiProcedure() {
            @Override
            public boolean doPhi(Phi phi) {
                rewriteWord(phi);
                return true;
            }
        });

        for (Instruction instr = block; instr != null; instr = instr.next()) {
            rewriteWord(instr);
        }
    }

    private void rewriteWord(Value value) {
        if (value.kind == CiKind.Object && isWord(value)) {
            value.kind = WordUtil.archKind();
        }
    }

    private boolean isWord(Value value) {
        if (value instanceof Phi) {
            phiVisited.clear();
            return isPhiWord((Phi) value);

        } else if (value instanceof Constant) {
            Constant c = (Constant) value;
            assert c.value.kind == CiKind.Object || c.value.kind == WordUtil.archKind();
            return c.value.kind == WordUtil.archKind();

        } else if (value instanceof Return) {
            Return r = (Return) value;
            assert r.result() != null;
            return isWord(r.result());

        } else {
            RiType type;
            try {
                type = value.declaredType();
            } catch (Throwable t) {
                // TODO temporary workaround: We load classes too eagerly, and when the class is not present, we end up with an exception here.
                return false;
            }

            if (!(type instanceof ClassActor)) {
                return false;
            }
            ClassActor actor = (ClassActor) type;
            assert actor.kind == Kind.REFERENCE || actor.kind == Kind.WORD;
            return actor.kind == Kind.WORD;
        }
    }

    private BitSet phiVisited = new BitSet();

    private boolean isPhiWord(Phi phi) {
        phiVisited.set(phi.id());
        for (int i = 0; i < phi.inputCount(); i++) {
            Value v = phi.inputAt(i);
            if (v instanceof Phi) {
                if (!phiVisited.get(v.id())) {
                    return isPhiWord((Phi) v);
                }
            } else {
                return isWord(v);
            }
        }
        throw FatalError.unexpected("circle detection for phi functions failed");
    }
}
