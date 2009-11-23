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
package com.sun.c1x.ci;

import java.util.ArrayList;
import java.util.List;

import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.ri.RiType;

/**
 * This interface represents the result which encapsulates compiler output, including
 * the compiled machine code, associated data and references, relocation information,
 * deoptimization information, etc.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class CiTargetMethod {

    /**
     * Represents a code position with associated additional information.
     */
    public abstract static class Site {
        public final int codePos;

        public Site(int codePos) {
            this.codePos = codePos;
        }
    }

    /**
     * Represents a safepoint and stores the register and stack reference map.
     */
    public static final class Safepoint extends Site {
        public final boolean[] registerMap;
        public final boolean[] stackMap;

        private Safepoint(int codePos, boolean[] registerMap, boolean[] stackMap) {
            super(codePos);
            this.registerMap = registerMap;
            this.stackMap = stackMap;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Safepoint at ");
            sb.append(codePos);
            sb.append(mapToString("registerMap", registerMap));
            sb.append(mapToString("stackMap", stackMap));
            return sb.toString();
        }
    }

    /**
     * Represents a call in the code and includes a stack reference map and optionally a register reference map. The
     * call can either be a runtime call, a global stub call or a call to a normal method.
     */
    public static final class Call extends Site {

        public final CiRuntimeCall runtimeCall;
        public final RiMethod method;
        public final Object globalStubID;

        public final boolean[] stackMap;
        public final boolean[] registerMap;

        private Call(int codePos, CiRuntimeCall runtimeCall, RiMethod method, Object globalStubID, boolean[] registerMap, boolean[] stackMap) {
            super(codePos);
            this.runtimeCall = runtimeCall;
            this.method = method;
            this.stackMap = stackMap;
            this.globalStubID = globalStubID;
            this.registerMap = registerMap;
        }

        @Override
        public String toString() {

            StringBuffer sb = new StringBuffer();
            if (runtimeCall != null) {
                sb.append("Runtime call to ");
                sb.append(runtimeCall.name());
            } else if (globalStubID != null) {
                sb.append("Global stub call to ");
                sb.append(globalStubID);
            } else {
                assert method != null;
                sb.append("Method call to ");
                sb.append(method.toString());
            }

            sb.append(" at pos ");
            sb.append(codePos);

            if (stackMap != null) {
                sb.append(mapToString("stackMap", stackMap));
            }

            if (registerMap != null) {
                sb.append(mapToString("registerMap", registerMap));
            }

            return sb.toString();
        }
    }

    /**
     * Represents a reference to data from the code. The associated data can be any constant.
     */
    public static final class DataPatch extends Site {
        public final CiConstant data;

        private DataPatch(int codePos, CiConstant data) {
            super(codePos);
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("Data patch site at pos %d referring to data %s", codePos, data);
        }
    }

    /**
     * Represents exception handler information for a specific code position. It includes the catch code position as
     * well as the caught exception type.
     */
    public static final class ExceptionHandler extends Site {
        public final int handlerPos;
        public final RiType exceptionType;

        private ExceptionHandler(int codePos, int handlerPos, RiType exceptionType) {
            super(codePos);
            this.handlerPos = handlerPos;
            this.exceptionType = exceptionType;
        }

        @Override
        public String toString() {
            return String.format("Exception edge from pos %d to %d with type %s", codePos, handlerPos, (exceptionType == null) ? "null" : exceptionType.javaClass().getName());
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
     * Records a reference map at a call location in the code array.
     *
     * @param codePosition the position in the code array
     * @param runtimeCall  the runtime call
     * @param stackMap     the bitmap that indicates which stack locations
     */
    public void recordRuntimeCall(int codePosition, CiRuntimeCall runtimeCall, boolean[] stackMap) {
        directCalls.add(new Call(codePosition, runtimeCall, null, null, null, stackMap));
    }

    /**
     * Records a global stub call in the code array.
     *
     * @param codePosition the position of the start of the call instruction in the code array
     * @param globalStubCallID the object identifying the global stub
     * @param registerMap the register reference map for the call site (may be null)
     * @param stackMap the stack reference map for the call site
     */
    public void recordGlobalStubCall(int codePosition, Object globalStubCallID, boolean[] registerMap, boolean[] stackMap) {
        directCalls.add(new Call(codePosition, null, null, globalStubCallID, registerMap, stackMap));
    }

    /**
     * Records a reference to the data section in the code section (e.g. to load an integer or floating point constant).
     *
     * @param codePosition the position in the code where the data reference occurs
     * @param data the data that is referenced
     */
    public void recordDataReference(int codePosition, CiConstant data) {
        assert codePosition >= 0 && data != null;
        dataReferences.add(new DataPatch(codePosition, data));
    }

    /**
     * Records a direct method call to the specified method in the code.
     *
     * @param codePosition the position in the code array
     * @param method the method being called
     * @param stackMap the bitmap that indicates which stack locations
     * @param direct true if this is a direct call, false otherwise
     */
    public void recordCall(int codePosition, RiMethod method, boolean[] stackMap, boolean direct) {
        final Call callSite = new Call(codePosition, null, method, null, null, stackMap);
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
    public void recordExceptionHandler(int codePos, int handlerPos, RiType throwableType) {
        exceptionHandlers.add(new ExceptionHandler(codePos, handlerPos, throwableType));
    }

    /**
     * Records the reference maps at a safepoint location in the code array.
     *
     * @param codePosition the position in the code array
     * @param registerMap  the bitmap that indicates which registers are references
     * @param stackMap     the bitmap that indicates which stack locations
     *                     are references
     */
    public void recordSafepoint(int codePosition, boolean[] registerMap, boolean[] stackMap) {
        safepoints.add(new Safepoint(codePosition, registerMap, stackMap));
        assert referenceRegisterCount == registerMap.length : "compiler produced register maps of different sizes";
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

    private static String mapToString(String name, boolean[] map) {
        StringBuffer sb = new StringBuffer();
        sb.append(' ');
        sb.append(name);
        sb.append('[');
        for (boolean b : map) {
            sb.append(b ? '1' : '0');
        }
        sb.append(']');
        return sb.toString();
    }
}
