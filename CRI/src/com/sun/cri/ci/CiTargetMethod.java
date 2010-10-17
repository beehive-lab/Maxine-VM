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
package com.sun.cri.ci;

import java.io.*;
import java.util.*;

import com.sun.cri.ri.*;

/**
 * Represents the output from compiling a method, including the compiled machine code, associated data and references,
 * relocation information, deoptimization information, etc. It is the essential component of a {@link CiResult}, which also includes
 * {@linkplain CiStatistics compilation statistics} and {@linkplain CiBailout failure information}.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class CiTargetMethod implements Serializable {

    /**
     * Represents a code position with associated additional information.
     */
    public abstract static class Site implements Serializable {
        public final int pcOffset;

        public Site(int pcOffset) {
            this.pcOffset = pcOffset;
        }
    }

    /**
     * Represents a safepoint and stores the register and stack reference map.
     */
    public static final class Safepoint extends Site {
        public final CiDebugInfo debugInfo;

        Safepoint(int pcOffset, CiDebugInfo debugInfo) {
            super(pcOffset);
            this.debugInfo = debugInfo;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Safepoint at ");
            sb.append(pcOffset);
            appendRefMap(sb, "registerMap", debugInfo.registerRefMap);
            appendRefMap(sb, "stackMap", debugInfo.frameRefMap);
            return sb.toString();
        }
    }

    /**
     * Represents a call in the code and includes a stack reference map and optionally a register reference map. The
     * call can either be a runtime call, a global stub call, a native call or a call to a normal method.
     */
    public static final class Call extends Site {
        public final CiRuntimeCall runtimeCall;
        public final RiMethod method;
        public final String symbol;
        public final Object globalStubID;

        public final CiDebugInfo debugInfo;
        public final byte[] stackMap;
        public final byte[] registerMap;

        Call(int pcOffset, CiRuntimeCall runtimeCall, RiMethod method, String symbol, Object globalStubID, byte[] registerMap, byte[] stackMap, CiDebugInfo debugInfo) {
            super(pcOffset);
            this.runtimeCall = runtimeCall;
            this.method = method;
            this.stackMap = stackMap;
            this.symbol = symbol;
            this.globalStubID = globalStubID;
            this.registerMap = registerMap;
            this.debugInfo = debugInfo;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (runtimeCall != null) {
                sb.append("Runtime call to ");
                sb.append(runtimeCall.name());
            } else if (symbol != null) {
                sb.append("Native call to ");
                sb.append(symbol);
            } else if (globalStubID != null) {
                sb.append("Global stub call to ");
                sb.append(globalStubID);
            } else {
                assert method != null;
                sb.append("Method call to ");
                sb.append(method.toString());
            }

            sb.append(" at pos ");
            sb.append(pcOffset);

            if (debugInfo != null) {
                appendRefMap(sb, "stackMap", debugInfo.frameRefMap);
                appendRefMap(sb, "registerMap", debugInfo.registerRefMap);
            }

            return sb.toString();
        }
    }

    /**
     * Represents a reference to data from the code. The associated data can be any constant.
     */
    public static final class DataPatch extends Site {
        public final CiConstant constant;

        DataPatch(int pcOffset, CiConstant data) {
            super(pcOffset);
            this.constant = data;
        }

        @Override
        public String toString() {
            return String.format("Data patch site at pos %d referring to data %s", pcOffset, constant);
        }
    }

    /**
     * Represents exception handler information for a specific code position. It includes the catch code position as
     * well as the caught exception type.
     */
    public static final class ExceptionHandler extends Site {
        public final int bci;
        public final int scopeLevel;
        public final int handlerPos;
        public final int handlerBci;
        public final RiType exceptionType;

        ExceptionHandler(int pcOffset, int bci, int scopeLevel, int handlerPos, int handlerBci, RiType exceptionType) {
            super(pcOffset);
            this.bci = bci;
            this.scopeLevel = scopeLevel;
            this.handlerPos = handlerPos;
            this.handlerBci = handlerBci;
            this.exceptionType = exceptionType;
        }

        @Override
        public String toString() {
            return String.format("Exception edge from pos %d to %d with type %s", pcOffset, handlerPos, (exceptionType == null) ? "null" : exceptionType);
        }
    }
    
    public static final class Mark extends Site {
        public final Object id;
        public final Mark[] references;

        Mark(int pcOffset, Object id, Mark[] references) {
            super(pcOffset);
            this.id = id;
            this.references = references;
        }

        @Override
        public String toString() {
            if (id == null)
                return String.format("Mark at pos %d with %d references", pcOffset, references.length);
            else if(id instanceof Integer) {
                return String.format("Mark at pos %d with %d references and id %s", pcOffset, references.length, Integer.toHexString((Integer)id));
            } else {
                return String.format("Mark at pos %d with %d references and id %s", pcOffset, references.length, id.toString());
            }
        }
    }

    /**
     * List of safepoints in the code.
     */
    public final List<Safepoint> safepoints = new ArrayList<Safepoint>();

    /**
     * List of direct calls in the code.
     */
    public final List<Call> directCalls = new ArrayList<Call>();

    /**
     * List of indirect calls in the code.
     */
    public final List<Call> indirectCalls = new ArrayList<Call>();

    /**
     * List of data references in the code.
     */
    public final List<DataPatch> dataReferences = new ArrayList<DataPatch>();

    /**
     * List of exception handlers in the code.
     */
    public final List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();
    
    public final List<Mark> marks = new ArrayList<Mark>();
    

    private final int referenceRegisterCount;
    private int frameSize = -1;
    private int registerRestoreEpilogueOffset = -1;
    private byte[] targetCode;
    private int targetCodeSize;

    /**
     * Constructs a new target method.
     *
     * @param referenceRegisterCount the number of registers in the register reference maps
     */
    public CiTargetMethod(int referenceRegisterCount) {
        this.referenceRegisterCount = referenceRegisterCount;
    }

    /**
     * Sets the frame size in bytes. Does not include the return address pushed onto the
     * stack, if any.
     *
     * @param size the size of the frame in bytes
     */
    public void setFrameSize(int size) {
        frameSize = size;
    }

    /**
     * Sets the machine that has been generated by the compiler.
     *
     * @param code the machine code generated
     * @param size the size of the machine code
     */
    public void setTargetCode(byte[] code, int size) {
        targetCode = code;
        targetCodeSize = size;
    }

    /**
     * Records a reference to the data section in the code section (e.g. to load an integer or floating point constant).
     *
     * @param codePos the position in the code where the data reference occurs
     * @param data the data that is referenced
     */
    public void recordDataReference(int codePos, CiConstant data) {
        assert codePos >= 0 && data != null;
        dataReferences.add(new DataPatch(codePos, data));
    }

    /**
     * Records a direct method call to the specified method in the code.
     *
     * @param codePos the position in the code array
     * @param target the {@linkplain RiMethod method}, {@linkplain CiRuntimeCall runtime call}, {@linkplain String native function} or stub being called
     * @param debugInfo the debug info for the call site
     * @param stackMap the bitmap that indicates which stack locations
     * @param direct true if this is a direct call, false otherwise
     */
    public void recordCall(int codePos, Object target, CiDebugInfo debugInfo, byte[] stackMap, boolean direct) {
        CiRuntimeCall rt = target instanceof CiRuntimeCall ? (CiRuntimeCall) target : null;
        RiMethod meth = target instanceof RiMethod ? (RiMethod) target : null;
        String symbol = target instanceof String ? (String) target : null;
        // make sure that only one is non-null
        Object globalStubID = (rt == null && meth == null && symbol == null) ? target : null;
        
        final Call callSite = new Call(codePos, rt, meth, symbol, globalStubID, null, stackMap, debugInfo);
        if (direct) {
            directCalls.add(callSite);
        } else {
            indirectCalls.add(callSite);
        }
    }

    /**
     * Records an exception handler for this method.
     *
     * @param codePos  the position in the code that is covered by the handler
     * @param handlerPos    the position of the handler
     * @param throwableType the type of exceptions handled by the handler
     */
    public void recordExceptionHandler(int codePos, int bci, int scopeLevel, int handlerPos, int handlerBci, RiType throwableType) {
        exceptionHandlers.add(new ExceptionHandler(codePos, bci, scopeLevel, handlerPos, handlerBci, throwableType));
    }

    /**
     * Records the reference maps at a safepoint location in the code array.
     *
     * @param codePos the position in the code array
     * @param registerMap  the bitmap that indicates which registers are references
     * @param stackMap     the bitmap that indicates which stack locations
     * @param debugInfo    the debug info for the safepoint site
     */
    public void recordSafepoint(int codePos, byte[] registerMap, byte[] stackMap, CiDebugInfo debugInfo) {
        safepoints.add(new Safepoint(codePos, debugInfo));
        assert referenceRegisterCount <= registerMap.length * 8 : "compiler produced register maps of different sizes";
    }

    /**
     * Records an instruction mark within this method.
     *
     * @param codePos the position in the code that is covered by the handler
     * @param id the identifier for this mark
     * @param references an array of other marks that this mark references
     */
    public Mark recordMark(int codePos, Object id, Mark[] references) {
        Mark mark = new Mark(codePos, id, references);
        marks.add(mark);
        return mark;
    }
    
    /**
     * Allows a method to specify the offset of the epilogue that restores the callee saved registers. Must be called
     * iff the method is a callee saved method and stores callee registers on the stack.
     *
     * @param registerRestoreEpilogueOffset the offset in the machine code where the epilogue begins
     */
    public void setRegisterRestoreEpilogueOffset(int registerRestoreEpilogueOffset) {
        this.registerRestoreEpilogueOffset = registerRestoreEpilogueOffset;
    }

    /**
     * The frame size of the method in bytes.
     *
     * @return the frame size
     */
    public int frameSize() {
        assert frameSize != -1 : "frame size not yet initialized!";
        return frameSize;
    }

    /**
     * The size of the register reference map.
     *
     * @return the number of registers that can hold references
     */
    public int referenceRegisterCount() {
        return referenceRegisterCount;
    }

    /**
     * @return the code offset of the start of the epilogue that restores all callee saved registers, or -1 if this is
     *         not a callee saved method
     */
    public int registerRestoreEpilogueOffset() {
        return registerRestoreEpilogueOffset;
    }

    /**
     * @return the machine code generated for this method
     */
    public byte[] targetCode() {
        return targetCode;
    }

    /**
     * @return the size of the machine code generated for this method
     */
    public int targetCodeSize() {
        return targetCodeSize;
    }

    static void appendRefMap(StringBuilder sb, String name, byte[] map) {
        if (map != null) {
            sb.append(' ');
            sb.append(name);
            sb.append('[');
            for (byte b : map) {
                for (int j = 0; j < 8; j++) {
                    int z = (b >> j) & 1;
                    sb.append(z);
                }
            }
            sb.append(']');
        }
    }
}
