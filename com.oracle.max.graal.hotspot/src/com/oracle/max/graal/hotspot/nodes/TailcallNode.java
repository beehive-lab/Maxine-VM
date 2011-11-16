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
package com.oracle.max.graal.hotspot.nodes;

 import static com.sun.cri.ci.CiCallingConvention.Type.JavaCall;

import java.lang.reflect.Modifier;

import com.oracle.max.graal.compiler.gen.LIRGenerator;
import com.oracle.max.graal.hotspot.target.amd64.AMD64TailcallOpcode;
import com.oracle.max.graal.nodes.FixedWithNextNode;
import com.oracle.max.graal.nodes.FrameState;
import com.oracle.max.graal.nodes.spi.LIRGeneratorTool;
import com.oracle.max.graal.nodes.spi.LIRLowerable;
import com.sun.cri.ci.CiCallingConvention;
import com.sun.cri.ci.CiKind;
import com.sun.cri.ci.CiUtil;
import com.sun.cri.ci.CiValue;
import com.sun.cri.ri.RiCompiledMethod;
import com.sun.cri.ri.RiResolvedMethod;

/**
 * Performs a tail call to the specified target compiled method, with the parameter taken from the supplied FrameState.
 */
public class TailcallNode extends FixedWithNextNode implements LIRLowerable {

    @Input private final FrameState frameState;
    @Data private final RiCompiledMethod target;

    public TailcallNode(RiCompiledMethod target, FrameState frameState) {
        super(CiKind.Illegal);
        this.target = target;
        this.frameState = frameState;
    }

    @Override
    public void generate(LIRGeneratorTool generator) {
        LIRGenerator gen = (LIRGenerator) generator;

        RiResolvedMethod method = frameState.method();
        boolean isStatic = Modifier.isStatic(method.accessFlags());
        CiKind[] signature = CiUtil.signatureToKinds(method.signature(), isStatic ? null : method.holder().kind(true));
        CiCallingConvention cc = gen.compilation.registerConfig.getCallingConvention(JavaCall, signature, gen.compilation.compiler.target, false);

        CiValue[] parameters = new CiValue[cc.locations.length];
        int slot = 0;
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = gen.operand(frameState.localAt(slot++));
            if (parameters[i].kind == CiKind.Long) {
                slot++;
            }
        }

        gen.append(AMD64TailcallOpcode.TAILCALL.create(target, parameters, cc.locations));
    }
}
