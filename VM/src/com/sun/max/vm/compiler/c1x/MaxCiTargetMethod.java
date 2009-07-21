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
package com.sun.max.vm.compiler.c1x;

import com.sun.c1x.ci.*;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.actor.member.MethodActor;
import com.sun.max.vm.collect.ByteArrayBitMap;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.code.Code;
import com.sun.max.vm.VMConfiguration;
import com.sun.max.collect.AppendableSequence;
import com.sun.max.annotate.PROTOTYPE_ONLY;
import com.sun.max.unsafe.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the consumer of C1X compiler's output that creates the appropriate
 * Maxine data structures and installs them into the code region.
 *
 * @author Ben L. Titzer
 */
public class MaxCiTargetMethod implements CiTargetMethod {
    static class SafepointRefMap {
        final int codePos;
        final boolean[] registerMap;
        final boolean[] stackMap;

        SafepointRefMap(int codePos, boolean[] registerMap, boolean[] stackMap) {
            this.codePos = codePos;
            this.registerMap = registerMap;
            this.stackMap = stackMap;
        }
    }

    static class CallSite {
        final int codePos;
        final CiRuntimeCall runtimeCall;
        final CiMethod method;
        final boolean direct;
        final boolean[] stackMap;

        CallSite(int codePos, CiRuntimeCall runtimeCall, CiMethod method, boolean direct, boolean[] stackMap) {
            this.codePos = codePos;
            this.runtimeCall = runtimeCall;
            this.method = method;
            this.direct = direct;
            this.stackMap = stackMap;
        }
    }

    static class DataPatchSite {
        final int codePos;
        final int dataPos;

        DataPatchSite(int codePos, int dataPos) {
            this.codePos = codePos;
            this.dataPos = dataPos;
        }
    }

    static class RefPatchSite {
        final int codePos;
        final Object referrent;
        Object[] array;
        int index;

        RefPatchSite(int codePos, Object referrent) {
            this.codePos = codePos;
            this.referrent = referrent;
        }
    }

    static class ExceptionHandler {
        final int codePosStart;
        final int codePosEnd;
        final int handlerPos;
        final CiType exceptionType;

        ExceptionHandler(int codePosStart, int codePosEnd, int handlerPos, CiType exceptionType) {
            this.codePosStart = codePosStart;
            this.codePosEnd = codePosEnd;
            this.handlerPos = handlerPos;
            this.exceptionType = exceptionType;
        }
    }

    public final ClassMethodActor classMethodActor;
    C1XTargetMethod targetMethod;

    int frameSize;
    int registerSize;
    byte[] targetCode;
    int targetCodeSize;
    byte[] data;
    int dataSize;
    int refSize;
    int safepoints;
    int runtimeCalls;
    int directCalls;
    int indirectCalls;

    final List<SafepointRefMap> safepointRefMaps = new ArrayList<SafepointRefMap>();
    final List<CallSite> callSites = new ArrayList<CallSite>();
    final List<DataPatchSite> dataPatchSites = new ArrayList<DataPatchSite>();
    final List<RefPatchSite> refPatchSites = new ArrayList<RefPatchSite>();
    final List<ExceptionHandler> exceptionHandlers = new ArrayList<ExceptionHandler>();

    CiDeoptimizer deoptimizer;

    /**
     * Creates a new compiler interface target method for the specified class method actor.
     *
     * @param classMethodActor the class method actor
     */
    public MaxCiTargetMethod(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
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
     * @param size the size of the code within the array
     */
    public void setTargetCode(byte[] code, int size) {
        targetCode = code;
        targetCodeSize = size;
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
        this.data = data;
        this.dataSize = size;
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
     * Records a reference map at a call location in the code array.
     *
     * @param codePosition the position in the code array
     * @param runtimeCall  the runtime call
     * @param stackMap     the bitmap that indicates which stack locations
     */
    public void recordRuntimeCall(int codePosition, CiRuntimeCall runtimeCall, boolean[] stackMap) {
        callSites.add(new CallSite(codePosition, runtimeCall, null, true, stackMap));
        directCalls++;
        assert stackMap.length == frameSize;
    }

    /**
     * Records a reference to the data section in the code section (e.g. to
     * load an integer or floating point constant).
     *
     * @param codePosition the position in the code where the data reference occurs
     * @param dataPosition the position in the data which is referred to
     */
    public void recordDataReferenceInCode(int codePosition, int dataPosition) {
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
        refSize++;
    }

    /**
     * Records a direct method call to the specified method in the code.
     * @param codePosition the position in the code array
     * @param method the method being called
     * @param stackMap the bitmap that indicates which stack locations
     */
    public void recordDirectCall(int codePosition, CiMethod method, boolean[] stackMap) {
        callSites.add(new CallSite(codePosition, null, method, true, stackMap));
        directCalls++;
        assert stackMap.length == frameSize : "compiler produced stack map that doesn't cover whole frame";
    }

    /**
     * Records an indirect method call to the specified method in the code.
     * @param codePosition the position in the code array
     * @param method the method being called
     * @param stackMap the bitmap that indicates which stack locations
     */
    public void recordIndirectCall(int codePosition, CiMethod method, boolean[] stackMap) {
        callSites.add(new CallSite(codePosition, null, method, false, stackMap));
        indirectCalls++;
        assert stackMap.length == frameSize : "compiler produced stack map that doesn't cover whole frame";
    }

    /**
     * Records an exception handler for this method.
     *
     * @param codePosStart  the start position in the code that is covered by the handler (inclusive)
     * @param codePosEnd    the end position covered by the handler (exclusive)
     * @param handlerPos    the position of the handler
     * @param throwableType the type of exceptions handled by the handler
     */
    public void recordExceptionHandler(int codePosStart, int codePosEnd, int handlerPos, CiType throwableType) {
        exceptionHandlers.add(new ExceptionHandler(codePosStart, codePosEnd, handlerPos, throwableType));
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

    /**
     * Finishes the compilation and installs the machine code into internal VM data structures.
     */
    public void finish() {
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(dataSize, refSize, targetCodeSize);

        final int numberOfStopPositions = directCalls + indirectCalls + safepoints;
        final int[] stopPositions = new int[numberOfStopPositions];
        final byte[] refMaps = new byte[TargetMethod.computeReferenceMapsSize(directCalls, indirectCalls, safepoints, stackRefMapSize(), registerRefMapSize())];
        final ByteArrayBitMap bitMap = new ByteArrayBitMap(refMaps);
        final Object[] refLiterals = new Object[refSize];

        targetMethod = new C1XTargetMethod(classMethodActor);
        Code.allocate(targetBundleLayout, targetMethod);

        ClassMethodActor[] directCallees = processCallSites(stopPositions, bitMap);
        processSafepoints(stopPositions, bitMap);

        processDataPatches(targetBundleLayout);
        processRefPatches(targetBundleLayout, refLiterals);

        // TODO: encode exception handler information
        int[] catchRangePositions = null;
        int[] catchBlockPositions = null;
        // TODO: encode deopt info
        byte[] compressedJavaFrameDescriptors = null;
        byte[] encodedInlineDataDescriptors = null;
        TargetABI abi = VMConfiguration.target().targetABIsScheme().optimizedJavaABI();

        targetMethod.setGenerated(
            catchRangePositions,
            catchBlockPositions,
            stopPositions,
            compressedJavaFrameDescriptors,
            directCallees,
            indirectCalls,
            safepoints,
            refMaps,
            data,
            refLiterals,
            targetCode,
            encodedInlineDataDescriptors,
            frameSize,
            stackRefMapSize(),
            abi
        );
    }

    private void processRefPatches(TargetBundleLayout bundleLayout, Object[] refLiterals) {
        if (!refPatchSites.isEmpty()) {
            Offset dataStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.referenceLiterals);
            Offset codeStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.code);
            Offset diff = dataStart.minus(codeStart).asOffset();
            int refPatchPos = 0;
            for (RefPatchSite refPatch : refPatchSites) {
                refLiterals[refPatchPos++] = refPatch.referrent;
                int refSize = Word.size(); // TODO: Use C1X target object
                patchRelativeInstruction(refPatch.codePos, diff.plus(refPatch.index * refSize - refPatch.codePos));
            }
        }
    }

    private void processDataPatches(TargetBundleLayout bundleLayout) {
        if (!dataPatchSites.isEmpty()) {
            Offset dataStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.scalarLiterals);
            Offset codeStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.code);
            Offset diff = dataStart.minus(codeStart).asOffset();
            for (DataPatchSite dataPatch : dataPatchSites) {
                patchRelativeInstruction(dataPatch.codePos, diff.plus(dataPatch.dataPos - dataPatch.codePos));
            }
        }
    }

    private void patchRelativeInstruction(int codePos, Offset relative) {
        // TODO: patch relative load instructions in a platform-dependent way
    }

    private void processSafepoints(int[] stopPositions, ByteArrayBitMap bitMap) {
        int safepointPos = directCalls + indirectCalls;
        bitMap.setSize(stackRefMapSize());
        bitMap.setIndex(safepointPos);
        for (SafepointRefMap refMap : safepointRefMaps) {
            // fill in the stop position for this safepoint
            stopPositions[safepointPos++] = refMap.codePos;
            setBits(bitMap, refMap.stackMap);
        }
        bitMap.setSize(registerRefMapSize());
        for (SafepointRefMap refMap : safepointRefMaps) {
            // fill in the register map
            setBits(bitMap, refMap.registerMap);
        }
    }

    private ClassMethodActor[] processCallSites(int[] stopPositions, ByteArrayBitMap bitMap) {
        int directPos = 0;
        int indirectPos = directCalls;
        bitMap.setSize(stackRefMapSize());
        final ClassMethodActor[] directCallees = new ClassMethodActor[directCalls];
        for (CallSite callSite : callSites) {
            // fill in the stop position for this call site
            if (callSite.direct) {
                bitMap.setOffset(directPos);
                directCallees[directPos] = getClassMethodActor(callSite.runtimeCall, callSite.method);
                stopPositions[directPos] = callSite.codePos;
                directPos++;
            } else {
                bitMap.setOffset(indirectPos);
                stopPositions[indirectPos] = callSite.codePos;
                indirectPos++;
            }
            // fill in the reference map for this call site
            setBits(bitMap, callSite.stackMap);
            assert directPos <= indirectCalls;
            assert indirectPos <= directCalls + indirectCalls;
        }
        return directCallees;
    }

    private ClassMethodActor getClassMethodActor(CiRuntimeCall runtimeCall, CiMethod method) {
        if (method != null) {
            final MaxCiMethod maxMethod = (MaxCiMethod) method;
            return maxMethod.asClassMethodActor("directCall()");
        }
        // TODO: get the class method actor for a runtime method call
        return null;
    }

    private void setBits(ByteArrayBitMap bitMap, boolean[] stackMap) {
        for (int i = 0; i < stackMap.length; i++) {
            if (stackMap[i]) {
                bitMap.set(i);
            }
        }
        bitMap.next();
    }

    private int stackRefMapSize() {
        return ((this.frameSize + 7) >> 3) << 3; // round up to next byte size
    }

    private int registerRefMapSize() {
        return ((this.registerSize + 7) >> 3) << 3; // round up to next byte size
    }

    @PROTOTYPE_ONLY
    void gatherCalls(AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        // iterate over all the calls and append them to the appropriate lists
        for (CallSite site : callSites) {
            if (site.method.isLoaded()) {
                MethodActor methodActor = ((MaxCiMethod) site.method).asMethodActor("gatherCalls()");
                if (site.direct) {
                    directCalls.append(getClassMethodActor(site.runtimeCall, site.method));
                } else {
                    if (site.method.holder().isInterface()) {
                        interfaceCalls.append(methodActor);
                    } else {
                        virtualCalls.append(methodActor);
                    }
                }
            }
        }
    }


    @Override
    public void recordCodeReferenceInData(int codePosition, int dataPosition, boolean relative) {
        // TODO Auto-generated method stub

    }


    @Override
    public void recordDataReferenceInCode(int codePosition, int dataPosition, boolean relative) {
        // TODO Auto-generated method stub

    }

}
