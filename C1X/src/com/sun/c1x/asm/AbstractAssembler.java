/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.asm;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.lir.LIRDebugInfo;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class AbstractAssembler {

    public final Buffer codeBuffer;
    public final CiTarget target;
    public final CiTargetMethod targetMethod;
    public final List<ExceptionInfo> exceptionInfoList;
    public final boolean is64;
    public final boolean is32;
    protected final int wordSize;

    public AbstractAssembler(CiTarget target) {
        this.target = target;
        this.targetMethod = new CiTargetMethod(target.allocatableRegs.registerRefMapSize);
        this.codeBuffer = new Buffer(target.arch.byteOrder);
        this.is32 = target.arch.is32bit();
        this.is64 = target.arch.is64bit();
        this.exceptionInfoList = new ArrayList<ExceptionInfo>();
        this.wordSize = target.arch.wordSize;
    }

    public final void bind(Label l) {
        if (l.isBound()) {
            // Assembler can bind a label more than once to the same place.
            assert l.position() == codeBuffer.position() : "attempt to redefine label";
        } else {
            // bind the label and patch any references to it
            l.bind(codeBuffer.position());
            l.patchInstructions(this);
        }
    }

    public void setFrameSize(int frameSize) {
        targetMethod.setFrameSize(frameSize);
    }

    public CiTargetMethod finishTargetMethod(RiRuntime runtime, int registerRestoreEpilogueOffset) {
        // Install code, data and frame size
        targetMethod.setTargetCode(codeBuffer.finished(), codeBuffer.position());
        targetMethod.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        // Record exception handlers if they exist
        if (exceptionInfoList != null) {
            for (ExceptionInfo ei : exceptionInfoList) {
                int codeOffset = ei.codeOffset;
                for (ExceptionHandler handler : ei.exceptionHandlers) {
                    int entryOffset = handler.entryCodeOffset();
                    RiType caughtType = handler.handler.catchKlass();
                    targetMethod.recordExceptionHandler(codeOffset, entryOffset, caughtType);
                }
            }
        }

        if (C1XOptions.PrintMetrics) {
            C1XMetrics.TargetMethods++;
            C1XMetrics.CodeBytesEmitted += targetMethod.targetCodeSize();
            C1XMetrics.SafepointsEmitted += targetMethod.safepoints.size();
            C1XMetrics.DirectCallSitesEmitted += targetMethod.directCalls.size();
            C1XMetrics.IndirectCallSitesEmitted += targetMethod.indirectCalls.size();
            C1XMetrics.DataPatches += targetMethod.dataReferences.size();
            C1XMetrics.ExceptionHandlersEmitted += targetMethod.exceptionHandlers.size();
        }

        if (C1XOptions.PrintAssembly) {
            Util.printSection("Target Method", Util.SECTION_CHARACTER);
            TTY.println("Frame size: %d", targetMethod);
            TTY.println("Frame size: %d", targetMethod.frameSize());
            TTY.println("Register size: %d", targetMethod.referenceRegisterCount());

            Util.printSection("Code", Util.SUB_SECTION_CHARACTER);
            Util.printBytes("Code", targetMethod.targetCode(), targetMethod.targetCodeSize(), C1XOptions.PrintAssemblyBytesPerLine);

            Util.printSection("Disassembly", Util.SUB_SECTION_CHARACTER);
            TTY.println(runtime.disassemble(targetMethod));

            Util.printSection("Safepoints", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Safepoint x : targetMethod.safepoints) {
                TTY.println(x.toString());
            }

            Util.printSection("Direct Call Sites", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Call x : targetMethod.directCalls) {
                TTY.println(x.toString());
            }

            Util.printSection("Indirect Call Sites", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Call x : targetMethod.indirectCalls) {
                TTY.println(x.toString());
            }

            Util.printSection("Data Patches", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.DataPatch x : targetMethod.dataReferences) {
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
                exceptionInfoList.add(new ExceptionInfo(pcOffset, info.exceptionHandlers));
            }
        }
    }

    public void recordImplicitException(int pcOffset, LIRDebugInfo info) {
        // record an implicit exception point
        if (info != null) {
            targetMethod.recordSafepoint(pcOffset, info.registerRefMap(), info.stackRefMap(), info.debugInfo());
            recordExceptionHandlers(pcOffset, info);
        }
    }

    protected void recordDirectCall(int posBefore, int posAfter, Object target, LIRDebugInfo info) {
        byte[] stackMap = info != null ? info.stackRefMap() : null;
        CiDebugInfo debugInfo = info != null ? info.debugInfo() : null;
        targetMethod.recordCall(posBefore, target, debugInfo, stackMap, true);
    }

    protected void recordIndirectCall(int posBefore, int posAfter, Object target, LIRDebugInfo info) {
        byte[] stackMap = info != null ? info.stackRefMap() : null;
        CiDebugInfo debugInfo = info != null ? info.debugInfo() : null;
        targetMethod.recordCall(posBefore, target, debugInfo, stackMap, false);
    }

    protected void recordSafepoint(int pos, byte[] registerMap, byte[] stackMap, CiDebugInfo debugInfo) {
        targetMethod.recordSafepoint(pos, registerMap, stackMap, debugInfo);
    }

    public Address recordDataReferenceInCode(CiConstant data) {
        assert data != null;

        int pos = codeBuffer.position();

        if (C1XOptions.TraceRelocation) {
            TTY.print("Data reference in code: pos = %d, data = %s", pos, data.toString());
        }

        targetMethod.recordDataReference(pos, data);
        return Address.InternalRelocation;
    }

    protected int target(Label l) {
        if (l.isBound()) {
            return l.position();
        } else {
            int branchPc = codeBuffer.position();
            l.addPatchAt(branchPc);
            // Need to return a pc, doesn't matter what it is since it will be
            // replaced during resolution later.
            // Don't return null or badAddress, since branches shouldn't overflow.
            // Don't return base either because that could overflow displacements
            // for shorter branches. It will get checked when bound.
            return branchPc;
        }
    }

    public abstract void nop();

    public abstract void nullCheck(CiRegister r);

    public abstract void buildFrame(int initialFrameSizeInBytes);

    public abstract void align(int codeEntryAlignment);

    public abstract void patchJumpTarget(int branch, int target);

    public final void emitByte(int x) {
        codeBuffer.emitByte(x);
    }

    public final void emitShort(int x) {
        codeBuffer.emitShort(x);
    }

    public final void emitInt(int x) {
        codeBuffer.emitInt(x);
    }

    public final void emitLong(long x) {
        codeBuffer.emitLong(x);
    }

    public void blockComment(String st) {
        Util.nonFatalUnimplemented();
    }

    public void verifiedEntry() {
        Util.nonFatalUnimplemented();
    }
}
