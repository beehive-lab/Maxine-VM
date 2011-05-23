/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ci;

import java.io.*;
import java.util.*;

import com.sun.cri.ri.*;

/**
 * Represents the output from compiling a method, including the compiled machine code, associated data and references,
 * relocation information, deoptimization information, etc. It is the essential component of a {@link CiResult}, which also includes
 * {@linkplain CiStatistics compilation statistics} and {@linkplain CiBailout failure information}.
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

        public CiDebugInfo debugInfo() {
            return null;
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
        public CiDebugInfo debugInfo() {
            return debugInfo;
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

        Call(int pcOffset, CiRuntimeCall runtimeCall, RiMethod method, String symbol, Object globalStubID, CiDebugInfo debugInfo) {
            super(pcOffset);
            this.runtimeCall = runtimeCall;
            this.method = method;
            this.symbol = symbol;
            this.globalStubID = globalStubID;
            this.debugInfo = debugInfo;
        }

        @Override
        public CiDebugInfo debugInfo() {
            return debugInfo;
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
            } else if (method != null) {
                sb.append("Method call to ");
                sb.append(method.toString());
            } else {
                sb.append("Template call");
            }

            sb.append(" at pos ");
            sb.append(pcOffset);

            if (debugInfo != null) {
                appendRefMap(sb, "stackMap", debugInfo.frameRefMap);
                appendRefMap(sb, "registerMap", debugInfo.registerRefMap);
                appendDebugInfo(sb, debugInfo);
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
     * Provides extra information about instructions or data at specific positions in {@link CiTargetMethod#targetCode()}.
     * This is optional information that can be used to enhance a disassembly of the code.
     */
    public abstract static class CodeAnnotation implements Serializable {
        public final int position;

        public CodeAnnotation(int position) {
            this.position = position;
        }
    }

    /**
     * A string comment about one or more instructions at a specific position in the code.
     */
    public static final class CodeComment extends CodeAnnotation {
        public final String value;
        public CodeComment(int position, String comment) {
            super(position);
            this.value = comment;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + position + ": " + value;
        }
    }

    /**
     * Labels some inline data in the code.
     */
    public static final class InlineData extends CodeAnnotation {
        public final int size;
        public InlineData(int position, int size) {
            super(position);
            this.size = size;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + position + ": size=" + size;
        }
    }

    /**
     * Describes a table of signed offsets embedded in the code. The offsets are relative to the starting
     * address of the table. This type of table maybe generated when translating a multi-way branch
     * based on a key value from a dense value set (e.g. the {@code tableswitch} JVM instruction).
     *
     * The table is indexed by the contiguous range of integers from {@link #low} to {@link #high} inclusive.
     */
    public static final class JumpTable extends CodeAnnotation {
        /**
         * The low value in the key range (inclusive).
         */
        public final int low;

        /**
         * The high value in the key range (inclusive).
         */
        public final int high;

        /**
         * The size (in bytes) of each table entry.
         */
        public final int entrySize;

        public JumpTable(int position, int low, int high, int entrySize) {
            super(position);
            this.low = low;
            this.high = high;
            this.entrySize = entrySize;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + position + ": [" + low + " .. " + high + "]";
        }
    }

    /**
     * Describes a table of key and offset pairs. The offset in each table entry is relative to the address of
     * the table. This type of table maybe generated when translating a multi-way branch
     * based on a key value from a sparse value set (e.g. the {@code lookupswitch} JVM instruction).
     */
    public static final class LookupTable extends CodeAnnotation {
        /**
         * The number of entries in the table.
         */
        public final int npairs;

        /**
         * The size (in bytes) of entry's key.
         */
        public final int keySize;

        /**
         * The size (in bytes) of entry's offset value.
         */
        public final int offsetSize;

        public LookupTable(int position, int npairs, int keySize, int offsetSize) {
            super(position);
            this.npairs = npairs;
            this.keySize = keySize;
            this.offsetSize = offsetSize;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + position + ": [npairs=" + npairs + ", keySize=" + keySize + ", offsetSize=" + offsetSize + "]";
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
            if (id == null) {
                return String.format("Mark at pos %d with %d references", pcOffset, references.length);
            } else if (id instanceof Integer) {
                return String.format("Mark at pos %d with %d references and id %s", pcOffset, references.length, Integer.toHexString((Integer) id));
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

    private int frameSize = -1;
    private int customStackAreaOffset = -1;
    private int registerRestoreEpilogueOffset = -1;

    /**
     * The buffer containing the emitted machine code.
     */
    private byte[] targetCode;

    /**
     * The leading number of bytes in {@link #targetCode} containing the emitted machine code.
     */
    private int targetCodeSize;

    private ArrayList<CodeAnnotation> annotations;

    private CiAssumptions assumptions;

    /**
     * Constructs a new target method.
     */
    public CiTargetMethod() {
    }

    public void setAssumptions(CiAssumptions assumptions) {
        this.assumptions = assumptions;
    }

    public CiAssumptions assumptions() {
        return assumptions;
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
     * @param direct true if this is a direct call, false otherwise
     */
    public void recordCall(int codePos, Object target, CiDebugInfo debugInfo, boolean direct) {
        CiRuntimeCall rt = target instanceof CiRuntimeCall ? (CiRuntimeCall) target : null;
        RiMethod meth = target instanceof RiMethod ? (RiMethod) target : null;
        String symbol = target instanceof String ? (String) target : null;
        // make sure that only one is non-null
        Object globalStubID = (rt == null && meth == null && symbol == null) ? target : null;

        final Call callSite = new Call(codePos, rt, meth, symbol, globalStubID, debugInfo);
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
     * @param debugInfo    the debug info for the safepoint site
     */
    public void recordSafepoint(int codePos, CiDebugInfo debugInfo) {
        safepoints.add(new Safepoint(codePos, debugInfo));
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
     * @return the code offset of the start of the epilogue that restores all callee saved registers, or -1 if this is
     *         not a callee saved method
     */
    public int registerRestoreEpilogueOffset() {
        return registerRestoreEpilogueOffset;
    }

    /**
     * Offset in bytes for the custom stack area (relative to sp).
     * @return the offset in bytes
     */
    public int customStackAreaOffset() {
        return customStackAreaOffset;
    }

    /**
     * @see #customStackAreaOffset()
     * @param bytes
     */
    public void setCustomStackAreaOffset(int bytes) {
        customStackAreaOffset = bytes;
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

    /**
     * @return the code annotations or {@code null} if there are none
     */
    public List<CodeAnnotation> annotations() {
        return annotations;
    }

    public void addAnnotation(CodeAnnotation annotation) {
        assert annotation != null;
        if (annotations == null) {
            annotations = new ArrayList<CiTargetMethod.CodeAnnotation>();
        }
        annotations.add(annotation);
    }

    private static void appendDebugInfo(StringBuilder sb, CiDebugInfo info) {
        if (info != null && info.hasFrame()) {
            sb.append(" #locals=").append(info.frame().numLocals).append(" #expr=").append(info.frame().numStack);
            if (info.frame().numLocks > 0) {
                sb.append(" #locks=").append(info.frame().numLocks);
            }
        }
    }

    private static void appendRefMap(StringBuilder sb, String name, CiBitMap map) {
        if (map != null) {
            sb.append(' ').append(name).append('[').append(map.toBinaryString(-1)).append(']');
        }
    }
}
