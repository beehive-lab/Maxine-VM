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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the consumer of C1X compiler's output that creates the appropriate
 * Maxine data structures and installs them into the code region.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public class C1XTargetMethodGenerator {
    private final C1XCompilerScheme compilerScheme;
    private final ClassMethodActor classMethodActor;
    private final CiTargetMethod ciTargetMethod;
    private int directCalls;
    private int indirectCalls;
    private int safepoints;
    private int refSize;
    private C1XTargetMethod targetMethod;

    /**
     * Creates a new compiler interface target method for the specified class method actor.
     *
     * @param compilerScheme the compiler scheme that generated the method
     * @param classMethodActor the class method actor
     */
    public C1XTargetMethodGenerator(C1XCompilerScheme compilerScheme, ClassMethodActor classMethodActor, CiTargetMethod ciTargetMethod) {
        this.compilerScheme = compilerScheme;
        this.classMethodActor = classMethodActor;
        this.ciTargetMethod = ciTargetMethod;

        directCalls = ciTargetMethod.directCalls();
        indirectCalls = ciTargetMethod.indirectCalls();
        safepoints = ciTargetMethod.safepointRefMaps.size();
        refSize = ciTargetMethod.refPatchSites.size();
    }

    /**
     * Finishes the compilation and installs the machine code into internal VM data structures.
     */
    public C1XTargetMethod finish() {
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(ciTargetMethod.dataSize, refSize, ciTargetMethod.targetCodeSize);

        final int numberOfStopPositions = directCalls + indirectCalls + safepoints;
        final int[] stopPositions = new int[numberOfStopPositions];
        final byte[] refMaps = new byte[TargetMethod.computeReferenceMapsSize(directCalls, indirectCalls, safepoints, stackRefMapSize(), registerRefMapSize())];
        final ByteArrayBitMap bitMap = new ByteArrayBitMap(refMaps);
        final Object[] refLiterals = new Object[refSize];

        targetMethod = new C1XTargetMethod(classMethodActor, compilerScheme);
        Code.allocate(targetBundleLayout, targetMethod);

        Object[] directCallees = processCallSites(stopPositions, bitMap);
        processSafepoints(stopPositions, bitMap);

        // TODO: encode exception handler information
        int[] catchRangePositions = null;
        int[] catchBlockPositions = null;
        // TODO: encode deopt info
        byte[] compressedJavaFrameDescriptors = null;
        byte[] encodedInlineDataDescriptors = null;
        TargetABI abi = VMConfiguration.target().targetABIsScheme().optimizedJavaABI();

        assert ciTargetMethod.targetCode != null;

        targetMethod.setGenerated(
            catchRangePositions,
            catchBlockPositions,
            stopPositions,
            compressedJavaFrameDescriptors,
            directCallees,
            indirectCalls,
            safepoints,
            refMaps,
            ciTargetMethod.data,
            refLiterals,
            ciTargetMethod.targetCode,
            encodedInlineDataDescriptors,
            ciTargetMethod.frameSize,
            stackRefMapSize(),
            abi
        );
        // TODO: patch code and data references
        processDataPatches(targetBundleLayout);
        processRefPatches(targetBundleLayout, targetMethod.referenceLiterals());

        return targetMethod;
    }

    private void processRefPatches(TargetBundleLayout bundleLayout, Object[] refLiterals) {
        if (!ciTargetMethod.refPatchSites.isEmpty()) {
            Offset dataStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.referenceLiterals);
            Offset codeStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.code);
            Offset diff = dataStart.minus(codeStart).asOffset();
            int refPatchPos = 0;
            for (CiTargetMethod.RefPatchSite refPatch : ciTargetMethod.refPatchSites) {
                refLiterals[refPatchPos++] = refPatch.referrent;
                int refSize = Word.size(); // TODO: Use C1X target object
                X86InstructionDecoder.patchRelativeInstruction(targetMethod.code(), refPatch.codePos, diff.plus(refPatch.index * refSize - refPatch.codePos).toInt());
            }
        }
    }

    private void processDataPatches(TargetBundleLayout bundleLayout) {
        if (!ciTargetMethod.dataPatchSites.isEmpty()) {
            Offset dataStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.scalarLiterals);
            Offset codeStart = bundleLayout.cellOffset(TargetBundleLayout.ArrayField.code);
            Offset diff = dataStart.minus(codeStart).asOffset();
            for (CiTargetMethod.DataPatchSite dataPatch : ciTargetMethod.dataPatchSites) {
                X86InstructionDecoder.patchRelativeInstruction(targetMethod.code(), dataPatch.codePos, diff.plus(dataPatch.dataPos - dataPatch.codePos).toInt());
            }
        }
    }

    private void processSafepoints(int[] stopPositions, ByteArrayBitMap bitMap) {
        int safepointPos = directCalls + indirectCalls;

        if (safepointPos >= stopPositions.length) {
            return;
        }

        bitMap.setSize(stackRefMapSize());
        bitMap.setIndex(safepointPos);
        for (CiTargetMethod.SafepointRefMap refMap : ciTargetMethod.safepointRefMaps) {
            // fill in the stop position for this safepoint
            stopPositions[safepointPos++] = refMap.codePos;
            setBits(bitMap, refMap.stackMap);
        }
        bitMap.setSize(registerRefMapSize());
        for (CiTargetMethod.SafepointRefMap refMap : ciTargetMethod.safepointRefMaps) {
            // fill in the register map
            setBits(bitMap, refMap.registerMap);
        }
    }

    private Object[] processCallSites(int[] stopPositions, ByteArrayBitMap bitMap) {
        int directPos = 0;
        int indirectPos = directCalls;
        bitMap.setSize(stackRefMapSize());
        final List<Object> directCallees = new ArrayList<Object>(directCalls);
        for (CiTargetMethod.CallSite callSite : ciTargetMethod.callSites) {
            // fill in the stop position for this call site
            if (callSite.direct) {
                bitMap.setOffset(directPos);
                if (callSite.globalStubID != null) {
                    TargetMethod globalStubMethod = (TargetMethod) callSite.globalStubID;
                    assert globalStubMethod != null;
                    directCallees.add(globalStubMethod);
                } else {
                    final ClassMethodActor cma = getClassMethodActor(callSite.runtimeCall, callSite.method);
                    if (cma != null) {
                        directCallees.add(cma);
                    } else {
                        Trace.line(1, "Warning: Unresolved direct call: " + callSite.runtimeCall + ", " + callSite.method);
                    }
                }

                stopPositions[directPos] = callSite.codePos;
                directPos++;
            } else {
                bitMap.setOffset(indirectPos);
                stopPositions[indirectPos] = callSite.codePos;
                indirectPos++;
            }
            // fill in the reference map for this call site
            setBits(bitMap, callSite.stackMap);
            assert directPos <= directCalls;
            assert indirectPos <= directCalls + indirectCalls;
        }
        return directCallees.toArray(new Object[directCallees.size()]);
    }

    private ClassMethodActor getClassMethodActor(CiRuntimeCall runtimeCall, CiMethod method) {


        if (method != null) {
            final MaxCiMethod maxMethod = (MaxCiMethod) method;
            return maxMethod.asClassMethodActor("directCall()");
        }

        assert runtimeCall != null : "A call can either be a call to a method or a runtime call";
        return C1XRuntimeCalls.getClassMethodActor(runtimeCall);
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
        return ((ciTargetMethod.frameSize + 7) >> 3) << 3; // round up to next byte size
    }

    private int registerRefMapSize() {
        return ((ciTargetMethod.registerSize + 7) >> 3) << 3; // round up to next byte size
    }

    @PROTOTYPE_ONLY
    void gatherCalls(AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        // iterate over all the calls and append them to the appropriate lists
        for (CiTargetMethod.CallSite site : ciTargetMethod.callSites) {
            if (site.method.isLoaded()) {
                MethodActor methodActor = ((MaxCiMethod) site.method).asMethodActor("gatherCalls()");
                if (site.direct) {
                    if (site.globalStubID == null) {
                        directCalls.append(getClassMethodActor(site.runtimeCall, site.method));
                    }
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
}
