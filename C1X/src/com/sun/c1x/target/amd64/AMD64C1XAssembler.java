package com.sun.c1x.target.amd64;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;


public class AMD64C1XAssembler extends AMD64MacroAssembler {
    public final CiTargetMethod targetMethod;

    public List<ExceptionInfo> exceptionInfoList;

    public AMD64C1XAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
        this.targetMethod = new CiTargetMethod();
    }

    public void setFrameSize(int frameSize) {
        targetMethod.setFrameSize(frameSize);
    }

    public CiTargetMethod.Mark recordMark(Object id, CiTargetMethod.Mark[] references) {
        return targetMethod.recordMark(codeBuffer.position(), id, references);
    }

    public void blockComment(String s) {
        targetMethod.addAnnotation(new CiTargetMethod.CodeComment(codeBuffer.position(), s));
    }

    public CiTargetMethod finishTargetMethod(Object name, RiRuntime runtime, int registerRestoreEpilogueOffset, boolean isStub) {
        // Install code, data and frame size
        targetMethod.setTargetCode(codeBuffer.close(false), codeBuffer.position());
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

        if (C1XOptions.PrintMetrics) {
            C1XMetrics.TargetMethods++;
            C1XMetrics.CodeBytesEmitted += targetMethod.targetCodeSize();
            C1XMetrics.SafepointsEmitted += targetMethod.safepoints.size();
            C1XMetrics.DirectCallSitesEmitted += targetMethod.directCalls.size();
            C1XMetrics.IndirectCallSitesEmitted += targetMethod.indirectCalls.size();
            C1XMetrics.DataPatches += targetMethod.dataReferences.size();
            C1XMetrics.ExceptionHandlersEmitted += targetMethod.exceptionHandlers.size();
        }

        if (C1XOptions.PrintAssembly && !TTY.isSuppressed() && !isStub) {
            Util.printSection("Target Method", Util.SECTION_CHARACTER);
            TTY.println("Name: " + name);
            TTY.println("Frame size: " + targetMethod.frameSize());
            TTY.println("Register size: " + target.arch.registerReferenceMapBitCount);

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

            Util.printSection("Direct Call Sites", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Call x : targetMethod.directCalls) {
                TTY.println(x.toString());
                if (noDis && x.debugInfo != null) {
                    TTY.println(CiUtil.indent(x.debugInfo.toString(), "  "));
                }
            }

            Util.printSection("Indirect Call Sites", Util.SUB_SECTION_CHARACTER);
            for (CiTargetMethod.Call x : targetMethod.indirectCalls) {
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

    protected void recordDirectCall(int posBefore, int posAfter, Object target, LIRDebugInfo info) {
        CiDebugInfo debugInfo = info != null ? info.debugInfo() : null;
        targetMethod.recordCall(posBefore, target, debugInfo, true);
    }

    protected void recordIndirectCall(int posBefore, int posAfter, Object target, LIRDebugInfo info) {
        CiDebugInfo debugInfo = info != null ? info.debugInfo() : null;
        targetMethod.recordCall(posBefore, target, debugInfo, false);
    }

    public void recordSafepoint(int pos, LIRDebugInfo info) {
        // safepoints always need debug info
        CiDebugInfo debugInfo = info.debugInfo();
        targetMethod.recordSafepoint(pos, debugInfo);
    }

    public CiAddress recordDataReferenceInCode(CiConstant data) {
        assert data != null;

        int pos = codeBuffer.position();

        if (C1XOptions.TraceRelocation) {
            TTY.print("Data reference in code: pos = %d, data = %s", pos, data.toString());
        }

        targetMethod.recordDataReference(pos, data);
        return CiAddress.Placeholder;
    }


    public final void nativeCall(CiRegister dst, String symbol, LIRDebugInfo info) {
        int before = codeBuffer.position();
        int encode = prefixAndEncode(dst.encoding);
        emitByte(0xFF);
        emitByte(0xD0 | encode);
        int after = codeBuffer.position();
        recordIndirectCall(before, after, symbol, info);
        recordExceptionHandlers(after, info);
    }

    public final int directCall(Object target, LIRDebugInfo info) {
        int before = codeBuffer.position();
        emitByte(0xE8);
        emitInt(0);
        int after = codeBuffer.position();
        recordDirectCall(before, after, target, info);
        recordExceptionHandlers(after, info);
        return before;
    }

    public final int directJmp(Object target) {
        int before = codeBuffer.position();
        emitByte(0xE9);
        emitInt(0);
        int after = codeBuffer.position();
        recordDirectCall(before, after, target, null);
        return before;
    }

    public final int indirectCall(CiRegister dst, Object target, LIRDebugInfo info) {
        int before = codeBuffer.position();
        int encode = prefixAndEncode(dst.encoding);

        emitByte(0xFF);
        emitByte(0xD0 | encode);
        int after = codeBuffer.position();
        recordIndirectCall(before, after, target, info);
        recordExceptionHandlers(after, info);
        return before;
    }

}
