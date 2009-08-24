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
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class AbstractAssembler {

    public final Buffer codeBuffer;
    public final Buffer dataBuffer;
    public final Target target;
    private final CiTargetMethod targetMethod = new CiTargetMethod();

    private final int doubleAlignment;
    private final int floatAlignment;
    private final int longAlignment;
    private final int longLongAlignment;
    private final int intAlignment;

    public AbstractAssembler(Target target) {
        this.target = target;
        this.codeBuffer = new Buffer(target.arch.bitOrdering);
        this.dataBuffer = new Buffer(target.arch.bitOrdering);
        doubleAlignment = target.arch.wordSize;
        floatAlignment = target.arch.wordSize;
        longAlignment = target.arch.wordSize;
        longLongAlignment = Long.SIZE / 8 * 2;
        intAlignment = target.arch.wordSize;
    }

    public void bind(Label l) {
        if (l.isBound()) {
            // Assembler can bind a label more than once to the same place.
            Util.guarantee(l.loc() == codeBuffer.position(), "attempt to redefine label");
            return;
        }
        l.bindLoc(codeBuffer.position());
        l.patchInstructions(this);
    }

    public CiTargetMethod finishTargetMethod(RiRuntime runtime, int framesize, List<ExceptionInfo> exceptionInfoList, int registerRestoreEpilogueOffset) {

        // Install code, data and frame size
        targetMethod.setTargetCode(codeBuffer.finished(), codeBuffer.position());
        targetMethod.setData(dataBuffer.finished(), dataBuffer.position());
        targetMethod.setFrameSize(framesize);
        targetMethod.setRegisterRestoreEpilogueOffset(registerRestoreEpilogueOffset);

        // Record exception handlers if existant
        if (exceptionInfoList != null) {
            for (ExceptionInfo ei : exceptionInfoList) {
                int codeOffset = ei.codeOffset;
                for (ExceptionHandler handler : ei.exceptionHandlers) {
                    int entryOffset = handler.entryCodeOffset();
                    RiType catchedType = handler.handler.catchKlass();
                    targetMethod.recordExceptionHandler(codeOffset, entryOffset, catchedType);
                }
            }
        }

        if (C1XOptions.PrintAssembly) {

            Util.printSection("Target Method", Util.SECTION_CHARACTER);
            TTY.println("Frame size: %d", framesize);
            TTY.println("Register size: %d", targetMethod.registerSize);

            Util.printSection("Code", Util.SUB_SECTION_CHARACTER);
            Util.printBytes("Code", targetMethod.targetCode, targetMethod.targetCodeSize, C1XOptions.BytesPerLine);

            Util.printSection("Disassembly", Util.SUB_SECTION_CHARACTER);
            TTY.println(runtime.disassemble(Arrays.copyOf(targetMethod.targetCode, targetMethod.targetCodeSize)));

            Util.printSection("Data", Util.SUB_SECTION_CHARACTER);
            Util.printBytes("Data", targetMethod.data, targetMethod.dataSize, C1XOptions.BytesPerLine);

            Util.printSection("Safepoints", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.SafepointRefMap x : targetMethod.safepointRefMaps) {
                TTY.println(x.toString());
            }

            Util.printSection("Call Sites", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.CallSite x : targetMethod.callSites) {
                TTY.println(x.toString());
            }

            Util.printSection("Data Patches", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.DataPatchSite x : targetMethod.dataPatchSites) {
                TTY.println(x.toString());
            }

            Util.printSection("Reference Patches", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.RefPatchSite x : targetMethod.refPatchSites) {
                TTY.println(x.toString());
            }

            Util.printSection("Exception Handlers", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.ExceptionHandler x : targetMethod.exceptionHandlers) {
                TTY.println(x.toString());
            }
        }

        return targetMethod;
    }


    protected void generateStackOverflowCheck() {
        if (C1XOptions.UseStackBanging) {
            // Each code entry causes one stack bang n pages down the stack where n
            // is configurable by StackBangPages. The setting depends on the maximum
            // depth of VM call stack or native before going back into java code,
            // since only java code can raise a stack overflow exception using the
            // stack banging mechanism. The VM and native code does not detect stack
            // overflow.
            // The code in JavaCalls.call() checks that there is at least n pages
            // available, so all entry code needs to do is bang once for the end of
            // this shadow zone.
            // The entry code may need to bang additional pages if the framesize
            // is greater than a page.

            int bangEnd = C1XOptions.StackShadowPages * target.pageSize;

            // This is how far the previous frame's stack banging extended.
            int bangEndSafe = bangEnd;
            int bangOffset = bangEndSafe;
            while (bangOffset <= bangEnd) {
                // Need at least one stack bang at end of shadow zone.
                bangStackWithOffset(bangOffset);
                bangOffset += target.pageSize;
            }
        } // end (UseStackBanging)
    }

    protected abstract void bangStackWithOffset(int bangOffset);

    protected void emitByte(int x) {
        codeBuffer.emitByte(x);
    }

    protected void emitShort(int x) {
        codeBuffer.emitShort(x);
    }

    protected void emitInt(int x) {
        codeBuffer.emitInt(x);
    }

    protected void recordGlobalStubCall(int pos, Object globalStubCall, boolean[] stackMap) {

        assert pos >= 0 && globalStubCall != null && stackMap != null;

        if (C1XOptions.TraceRelocation) {
            TTY.print("Global stub call: pos = %d, name = %s, stackMap.length = %d", pos, globalStubCall, stackMap.length);
        }

        if (targetMethod != null) {
            targetMethod.recordGlobalStubCall(pos, globalStubCall, stackMap);
        }
    }

    protected void recordDirectCall(int pos, RiMethod call, boolean[] stackMap) {

        assert pos >= 0 && call != null && stackMap != null;

        if (C1XOptions.TraceRelocation) {
            TTY.print("Direct call: pos = %d, name = %s, stackMap.length = %d", pos, call.name(), stackMap.length);
        }

        if (targetMethod != null) {
            targetMethod.recordDirectCall(pos, call, stackMap);
        }
    }

    protected void recordRuntimeCall(int pos, CiRuntimeCall call, boolean[] stackMap) {

        assert pos >= 0 && call != null && stackMap != null;

        if (C1XOptions.TraceRelocation) {
            TTY.print("Runtime call: pos = %d, name = %s, stackMap.length = %d", pos, call.name(), stackMap.length);
        }

        if (targetMethod != null) {
            targetMethod.recordRuntimeCall(pos, call, stackMap);
        }
    }

    protected void recordDataReferenceInCode(int pos, int dataOffset) {

        assert pos >= 0 && dataOffset >= 0;

        if (C1XOptions.TraceRelocation) {
            TTY.print("Object reference in code: pos = %d, dataOffset = %d", pos, dataOffset);
        }

        if (targetMethod != null) {
            targetMethod.recordDataReferenceInCode(pos, dataOffset);
        }
    }

    protected Address recordObjectReferenceInCode(Object obj) {
//        assert obj != null;

        if (C1XOptions.TraceRelocation) {
            TTY.print("Object reference in code: pos = %d, object= %s", codeBuffer.position(), obj);
        }

        if (targetMethod != null) {
            targetMethod.recordObjectReferenceInCode(codeBuffer.position(), obj);
        }

        return Address.InternalRelocation;
    }

    protected void emitLong(long x) {
        codeBuffer.emitLong(x);
    }

    protected int target(Label l) {
        int branchPc = codeBuffer.position();
        if (l.isBound()) {
            return l.loc();
        } else {
            l.addPatchAt(branchPc);
            // Need to return a pc, doesn't matter what it is since it will be
            // replaced during resolution later.
            // Don't return null or badAddress, since branches shouldn't overflow.
            // Don't return base either because that could overflow displacements
            // for shorter branches. It will get checked when bound.
            return branchPc;
        }
    }

    private Address makeInternalAddress(int disp) {
        recordDataReferenceInCode(codeBuffer.position(), disp);
        return Address.InternalRelocation;
    }

    public Address doubleConstant(double d) {
        dataBuffer.align(doubleAlignment);
        int pos = dataBuffer.emitDouble(d);
        return makeInternalAddress(pos);
    }

    public Address floatConstant(float f) {
        dataBuffer.align(floatAlignment);
        int pos = dataBuffer.emitFloat(f);
        return makeInternalAddress(pos);
    }

    public Address longConstant(long l) {
        dataBuffer.align(longAlignment);
        int pos = dataBuffer.emitLong(l);
        return makeInternalAddress(pos);
    }

    public Address longLongConstant(long lHigh, long lLow) {
        dataBuffer.align(longLongAlignment);
        int pos = dataBuffer.emitLongLong(lHigh, lLow);
        return makeInternalAddress(pos);
    }

    public Address intConstant(int l) {
        dataBuffer.align(intAlignment);
        int pos = dataBuffer.emitInt(l);
        return makeInternalAddress(pos);
    }

    public abstract void nop();

    public void blockComment(String st) {
        Util.nonFatalUnimplemented();
    }

    public abstract void nullCheck(Register r);

    public void verifiedEntry() {
        Util.nonFatalUnimplemented();
    }

    public abstract void buildFrame(int initialFrameSizeInBytes);

    public abstract void align(int codeEntryAlignment);

    public abstract void makeOffset(int offset);

    public abstract void patchJumpTarget(int branch, int target);
}
