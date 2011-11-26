/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.nodes;

import static com.oracle.max.vm.ext.graal.target.amd64.AMD64StackAllocateOpcode.*;

import com.oracle.max.graal.compiler.lir.FrameMap.StackBlock;
import com.oracle.max.graal.compiler.target.amd64.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.type.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Instruction implementing the semantics of {@link Bytecodes#ALLOCA}.
 */
public final class AllocaNode extends FixedWithNextNode implements AMD64LIRLowerable {

    @Data public final int size;
    @Data public final boolean refs;
    @Data public final RiResolvedType declaredType;

    public AllocaNode(int size, boolean refs, RiResolvedType declaredType) {
        super(StampFactory.forKind(declaredType.kind(false)));
        this.size = size;
        this.refs = refs;
        this.declaredType = declaredType;
    }

    @Override
    public void generateAmd64(AMD64LIRGenerator gen) {
        CiVariable result = gen.newVariable(kind());
        StackBlock stackBlock = gen.compilation.frameMap().reserveStackBlock(size, refs);
        gen.append(STACK_ALLOCATE.create(result, stackBlock));
        gen.setResult(this, result);
    }

    @Override
    public RiResolvedType declaredType() {
        return declaredType;
    }
}
