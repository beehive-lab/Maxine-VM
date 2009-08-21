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

import java.util.*;

import com.sun.c1x.*;

/**
 * This interface represents the result which encapsulates compiler output, including
 * the compiled machine code, associated data and references, relocation information,
 * deoptimization information, etc.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class CiTargetMethod {

    public static class SafepointRefMap {
        public final int codePos;
        public final boolean[] registerMap;
        public final boolean[] stackMap;

        SafepointRefMap(int codePos, boolean[] registerMap, boolean[] stackMap) {
            this.codePos = codePos;
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

    public static class CallSite {
        public final int codePos;
        public final CiRuntimeCall runtimeCall;
        public final RiMethod method;
        public final Object globalStubID;
        public final boolean direct;
        public final boolean[] stackMap;

        CallSite(int codePos, CiRuntimeCall runtimeCall, RiMethod method, Object globalStubID, boolean direct, boolean[] stackMap) {
            this.codePos = codePos;
            this.runtimeCall = runtimeCall;
            this.method = method;
            this.direct = direct;
            this.stackMap = stackMap;
            this.globalStubID = globalStubID;
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
                sb.append("Method call to " + method.toString());
            }

            sb.append(" at pos ");
            sb.append(codePos);

            if (direct) {
                sb.append(" (direct)");
            } else {
                sb.append(" (indirect)");
            }

            sb.append(mapToString("stackMap", stackMap));
            return sb.toString();
        }
    }

    public static class DataPatchSite {
        public final int codePos;
        public final int dataPos;

        DataPatchSite(int codePos, int dataPos) {
            this.codePos = codePos;
            this.dataPos = dataPos;
        }

        @Override
        public String toString() {
            return String.format("Data patch site at pos %d referring to data pos %d", codePos, dataPos);
        }
    }

    public static class RefPatchSite {
        public final int codePos;
        public final Object referrent;

        RefPatchSite(int codePos, Object referrent) {
            this.codePos = codePos;
            this.referrent = referrent;
        }

        @Override
        public String toString() {
            return String.format("Reference patch site at pos %d to object %s", codePos, referrent);
        }
    }

    public static class ExceptionHandler {
        public final int codePos;
        public final int handlerPos;
        public final RiType exceptionType;

        ExceptionHandler(int codePos, int handlerPos, RiType exceptionType) {
            this.codePos = codePos;
            this.handlerPos = handlerPos;
            this.exceptionType = exceptionType;
        }

        @Override
        public String toString() {
            return String.format("Exception edge from pos %d to %d with type %s", codePos, handlerPos, (exceptionType == null) ? "null" : exceptionType.javaClass().getName());
        }
    }

    public final List<SafepointRefMap> safepointRefMaps = new ArrayList<SafepointRefMap>();
    public final List<CallSite> callSites = new ArrayList<CallSite>();
    public final List<DataPatchSite> dataPatchSites = new ArrayList<DataPatchSite>();
    public final List<RefPatchSite> refPatchSites = new ArrayList<RefPatchSite>();
    public final List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();

    public int registerSize;
    public int frameSize;
    public byte[] targetCode;
    public int targetCodeSize;
    public byte[] data;
    public int dataSize;
    CiDeoptimizer deoptimizer;
    int directCalls;
    int indirectCalls;
    public Bailout bailout;

    // For statistics
    public int totalInstructions;

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
     * @param size the size of the code within the array
     */
    public void setTargetCode(byte[] code, int size) {
        assert code != null;
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
        callSites.add(new CallSite(codePosition, runtimeCall, null, null, true, stackMap));
        directCalls++;
        //assert stackMap.length == frameSize;
    }

    public void recordGlobalStubCall(int codePosition, Object globalStubCall, boolean[] stackMap) {
        callSites.add(new CallSite(codePosition, null, null, globalStubCall, true, stackMap));
        directCalls++;
        //assert stackMap.length == frameSize;
    }

    /**
     * Records a reference to the data section in the code section (e.g. to
     * load an integer or floating point constant).
     *
     * @param codePosition the position in the code where the data reference occurs
     * @param dataPosition the position in the data which is referred to
     * @param relative {@code true} if the reference is instruction-relative
     */
    public void recordDataReferenceInCode(int codePosition, int dataPosition) {
        assert codePosition >= 0 && dataPosition >= 0;
        dataPatchSites.add(new DataPatchSite(codePosition, dataPosition));
    }

    /**
     * Records an object reference in the code section and the object that is
     * referred to.
     *
     * @param codePosition the position in the code section
     * @param ref          the object that is referenced
     */
    public void recordObjectReferenceInCode(int codePosition, Object ref) {
        refPatchSites.add(new RefPatchSite(codePosition, ref));
    }

    /**
     * Records a direct method call to the specified method in the code.
     * @param codePosition the position in the code array
     * @param method the method being called
     * @param stackMap the bitmap that indicates which stack locations
     */
    public void recordDirectCall(int codePosition, RiMethod method, boolean[] stackMap) {
        callSites.add(new CallSite(codePosition, null, method, null, true, stackMap));
        directCalls++;
        //assert stackMap.length == frameSize : "compiler produced stack map that doesn't cover whole frame";
    }

    /**
     * Records an indirect method call to the specified method in the code.
     * @param codePosition the position in the code array
     * @param method the method being called
     * @param stackMap the bitmap that indicates which stack locations
     */
    public void recordIndirectCall(int codePosition, RiMethod method, boolean[] stackMap) {
        callSites.add(new CallSite(codePosition, null, method, null, false, stackMap));
        indirectCalls++;
        //assert stackMap.length == frameSize : "compiler produced stack map that doesn't cover whole frame";
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
     * Sets the data that has been generated by the compiler, which may
     * include binary representations of floating point and integer constants,
     * as well as object references.
     *
     * @param data the data generated
     * @param size the size of the data within the array
     */
    public void setData(byte[] data, int size) {
        if (size == 0) {
            this.data = null;
            this.dataSize = 0;
        } else {
            this.data = data;
            this.dataSize = size;
        }
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
        safepointRefMaps.add(new SafepointRefMap(codePosition, registerMap, stackMap));
        if (registerSize == 0) {
            registerSize = registerMap.length;
        }
        assert registerSize == registerMap.length : "compiler produced register maps of different sizes";
        assert stackMap.length == frameSize : "compiler produced stack map that doesn't cover whole frame";
    }


    /**
     * Attaches a {@link com.sun.c1x.ci.CiDeoptimizer deoptimizer} object to this method that will
     * handle deoptimization requests by the VM.
     *
     * @param deoptimizer the deoptimizer object for this method
     */
    public void attachDeoptimizer(CiDeoptimizer deoptimizer) {
        this.deoptimizer = deoptimizer;
    }

    public int directCalls() {
        return directCalls;
    }

    public int indirectCalls() {
        return indirectCalls;
    }

    public int totalInstructions() {
        return totalInstructions;
    }

}
