/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.c1x.asm;

import java.util.ArrayList;
import java.util.List;

import com.oracle.max.asm.AbstractAssembler;
import com.oracle.max.criutils.TTY;
import com.sun.c1x.C1XMetrics;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.ir.ExceptionHandler;
import com.sun.c1x.lir.LIRDebugInfo;
import com.sun.c1x.util.Util;
import com.sun.cri.ci.CiAddress;
import com.sun.cri.ci.CiConstant;
import com.sun.cri.ci.CiDebugInfo;
import com.sun.cri.ci.CiTargetMethod;
import com.sun.cri.ci.CiUtil;
import com.sun.cri.ri.RiResolvedMethod;
import com.sun.cri.ri.RiRuntime;
import com.sun.cri.ri.RiType;

public class TargetMethodAssembler {
    public final AbstractAssembler asm;
    public final CiTargetMethod targetMethod;
    public List<ExceptionInfo> exceptionInfoList;

    public TargetMethodAssembler(AbstractAssembler asm) {
        this.asm = asm;
        this.targetMethod = new CiTargetMethod();
    }

    public void setFrameSize(int frameSize) {
        targetMethod.setFrameSize(frameSize);
    }

    public CiTargetMethod.Mark recordMark(Object id, CiTargetMethod.Mark[] references) {
        return targetMethod.recordMark(asm.codeBuffer.position(), id, references);
    }

    public void blockComment(String s) {
        targetMethod.addAnnotation(new CiTargetMethod.CodeComment(asm.codeBuffer.position(), s));
    }

    /**
     * Create trampolines for architectures that require them.
     * @param method
     */
    private void maybeCreateTrampolines(Object method, RiRuntime runtime) {
        if (asm.target.arch.usesTrampolines()) {
            /*
             * The CiTargetMethod only knows about calls compiled in the method body minus the prologue.
             * The runtime adjusts the number of safepoints to account for a call to an adapter. This adjustment
             * also needs to be accounted for in the trampoline array.
             */
            int calls = targetMethod.numberOfCalls();

            /*
             * Adapter calls are only relevant for regular Java methods. Stubs don't have them
             * so we filter them out. In the case where a method is a Stub the 'method' parameter
             * to this function is a String.
             */
            if (method instanceof RiResolvedMethod) {
                calls += runtime.numberOfAdapterCalls((RiResolvedMethod) method);
            }
            byte [] trampolines = asm.trampolines(calls);
            targetMethod.setTrampolines(trampolines);
        }
    }

    public CiTargetMethod finishTargetMethod(Object name, RiRuntime runtime, int registerRestoreEpilogueOffset, boolean isStub) {
        // Install code, data and frame size
        targetMethod.setTargetCode(asm.codeBuffer.close(false), asm.codeBuffer.position());
        targetMethod.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        // Record exception handlers if they exist
        if (exceptionInfoList != null) {
            for (ExceptionInfo ei : exceptionInfoList) {
                int codeOffset = ei.codeOffset;
                for (ExceptionHandler handler : ei.exceptionHandlers) {
                    int entryOffset = handler.entryCodeOffset();
                    RiType caughtType = handler.handler.catchType();
                    targetMethod.recordExceptionHandler(codeOffset, ei.bci, handler.scopeCount(), entryOffset, handler.handlerBCI(), caughtType);
                }
            }
        }

        maybeCreateTrampolines(name, runtime);

        if (C1XOptions.PrintMetrics) {
            C1XMetrics.TargetMethods++;
            C1XMetrics.CodeBytesEmitted += targetMethod.targetCodeSize();
            C1XMetrics.SafepointsEmitted += targetMethod.safepoints.size();
            C1XMetrics.DataPatches += targetMethod.dataReferences.size();
            C1XMetrics.ExceptionHandlersEmitted += targetMethod.exceptionHandlers.size();
        }

        if (C1XOptions.PrintAssembly && !TTY.isSuppressed() && !isStub) {
            Util.printSection("Target Method", Util.SECTION_CHARACTER);
            TTY.println("Name: " + name);
            TTY.println("Frame size: " + targetMethod.frameSize());
            TTY.println("Register size: " + asm.target.arch.registerReferenceMapBitCount);

            if (C1XOptions.PrintCodeBytes) {
                Util.printSection("Code", Util.SUB_SECTION_CHARACTER);
                TTY.println("Code: %d bytes", targetMethod.targetCodeSize());
                Util.printBytes(0L, targetMethod.targetCode(), 0, targetMethod.targetCodeSize(), C1XOptions.PrintAssemblyBytesPerLine);
            }

            Util.printSection("Disassembly", Util.SUB_SECTION_CHARACTER);
            String disassembly = runtime.disassemble(targetMethod);
            TTY.println(disassembly);
            boolean noDis = disassembly == null || disassembly.length() == 0;

            Util.printSection("Safepoints", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Safepoint x : targetMethod.safepoints) {
                TTY.println(x.toString());
                if (noDis && x.debugInfo != null) {
                    TTY.println(CiUtil.indent(x.debugInfo.toString(), "  "));
                }
            }

            Util.printSection("Data Patches", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.DataPatch x : targetMethod.dataReferences) {
                TTY.println(x.toString());
            }

            Util.printSection("Marks", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Mark x : targetMethod.marks) {
                TTY.println(x.toString());
            }

            Util.printSection("Exception Handlers", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.ExceptionHandler x : targetMethod.exceptionHandlers) {
                TTY.println(x.toString());
            }
        }

        return targetMethod;
    }

    public void recordExceptionHandlers(int pcOffset, LIRDebugInfo info) {
        if (info != null) {
            if (info.exceptionHandlers != null) {
                if (exceptionInfoList == null) {
                    exceptionInfoList = new ArrayList<ExceptionInfo>(4);
                }
                exceptionInfoList.add(new ExceptionInfo(pcOffset, info.exceptionHandlers, info.state.bci));
            }
        }
    }

    public void recordImplicitException(int pcOffset, LIRDebugInfo info) {
        // record an implicit exception point
        if (info != null) {
            targetMethod.recordSafepoint(pcOffset, info.debugInfo());
            recordExceptionHandlers(pcOffset, info);
        }
    }

    public void recordDirectCall(int posBefore, int size, Object target, LIRDebugInfo info) {
        CiDebugInfo debugInfo = info != null ? info.debugInfo() : null;
        targetMethod.recordCall(posBefore, size, target, debugInfo, true);
    }

    public void recordIndirectCall(int posBefore, int size, Object target, LIRDebugInfo info) {
        CiDebugInfo debugInfo = info != null ? info.debugInfo() : null;
        targetMethod.recordCall(posBefore, size, target, debugInfo, false);
    }

    public void recordSafepoint(int pos, LIRDebugInfo info) {
        // safepoints always need debug info
        CiDebugInfo debugInfo = info.debugInfo();
        targetMethod.recordSafepoint(pos, debugInfo);
    }

    public CiAddress recordDataReferenceInCode(CiConstant data) {
        return recordDataReferenceInCode(data, 0);
    }

    public CiAddress recordDataReferenceInCode(CiConstant data, int alignment) {
        assert data != null;

        int pos = asm.codeBuffer.position();

        if (C1XOptions.TraceRelocation) {
            TTY.print("Data reference in code: pos = %d, data = %s", pos, data.toString());
        }

        targetMethod.recordDataReference(pos, data, alignment);
        return CiAddress.Placeholder;
    }
}
