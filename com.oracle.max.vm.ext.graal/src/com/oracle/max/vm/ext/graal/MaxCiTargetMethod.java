/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import java.util.*;

import com.oracle.graal.api.code.CompilationResult;
import com.sun.cri.ci.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.deopt.*;
/**
 * Represents a Graal {@link CompilationResult} as a {@link CiTargetMethod}.
 * The ultimate wrapper class!
 */
public class MaxCiTargetMethod extends com.sun.cri.ci.CiTargetMethod {

    static MaxCiTargetMethod create(CompilationResult graalCompilation) {
        MaxCiTargetMethod result = new MaxCiTargetMethod(graalCompilation);
        return result;
    }

    // TODO remove
    private byte[] codeCopy;

    protected MaxCiTargetMethod(CompilationResult gCompilation) {
        // Safepoints
        List<CompilationResult.Infopoint> gInfopoints = gCompilation.getInfopoints();
        for (com.oracle.graal.api.code.CompilationResult.Infopoint gInfopoint : gInfopoints) {
            CiDebugInfo ciDebugInfo = MaxDebugInfo.toCi(gInfopoint.debugInfo);
            if (gInfopoint instanceof com.oracle.graal.api.code.CompilationResult.Call) {
                com.oracle.graal.api.code.CompilationResult.Call gCall = (com.oracle.graal.api.code.CompilationResult.Call) gInfopoint;
                Object target;
                if (gCall.target instanceof MaxForeignCallLinkage) {
                    target = ((MaxForeignCallLinkage) gCall.target).getMethodActor();
                } else if (gCall.target instanceof MaxResolvedJavaMethod) {
                    target = MaxResolvedJavaMethod.getRiResolvedMethod((MaxResolvedJavaMethod) gCall.target);
                } else if (gCall.target instanceof /*MaxResolvedJavaMethod.Unresolved*/MaxJavaMethod) {
                    // This is the current hack for unresolved methods
                    target = MaxJavaMethod.getRiMethod((MaxJavaMethod) gCall.target);
                } else {
                    target = gCall.target;
                }
                Call call = new Call(target, gCall.pcOffset, gCall.size, gCall.direct, ciDebugInfo);
                this.addSafepoint(call);
            } else {
                this.addSafepoint(new Safepoint(gInfopoint.pcOffset, ciDebugInfo));
            }
        }
        // Code
        byte[] graalCode = gCompilation.getTargetCode();
        codeCopy = Arrays.copyOf(graalCode, graalCode.length);
        this.setTargetCode(graalCode, gCompilation.getTargetCodeSize());
        // Data
        List<CompilationResult.DataPatch> dataReferences = gCompilation.getDataReferences();
        for (com.oracle.graal.api.code.CompilationResult.DataPatch dataPatch : dataReferences) {
            this.recordDataReference(dataPatch.pcOffset, ConstantMap.toCi(dataPatch.constant), dataPatch.alignment);
        }
        // Exception handlers
        List<CompilationResult.ExceptionHandler> exceptionHandlers = gCompilation.getExceptionHandlers();
        for (CompilationResult.ExceptionHandler e : exceptionHandlers) {
            this.recordExceptionHandler(e.pcOffset, -1, -1, e.handlerPos, -1, ClassActor.fromJava(Throwable.class));
        }

        // Misc
//        this.setCustomStackAreaOffset(gCompilation.getCustomStackAreaOffset());
        this.setCustomStackAreaOffset(Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET); // TODO figure out how Graal should get this
        this.setFrameSize(gCompilation.getFrameSize());
        this.setRegisterRestoreEpilogueOffset(gCompilation.getRegisterRestoreEpilogueOffset());
    }


}
