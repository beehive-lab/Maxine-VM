/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.jit;

import static com.sun.max.vm.compiler.target.StopType.*;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.cps.jit.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.Stop.*;
import com.sun.max.vm.template.*;

/**
 * Represents the {@link StopType stop} related information pertaining to a method compiled by a
 * {@linkplain BytecodeToTargetTranslator template based JIT compiler}.
 *
 * @author Doug Simon
 */
public class Stops {

    public final int[] stopPositions;
    public final ClassMethodActor[] directCallees;
    public final int numberOfIndirectCalls;
    public final int numberOfSafepoints;
    public final byte[] referenceMaps;
    public final ByteArrayBitMap isDirectCallToRuntime;
    public final BytecodeStopsIterator bytecodeStopsIterator;

    Stops(ClassMethodActor[] directCallees,
           ByteArrayBitMap isDirectCallToRuntime,
           int numberOfIndirectCalls,
           int numberOfSafepoints,
           int[] stopPositions,
           BytecodeStopsIterator bytecodeStopsIterator,
           byte[] referenceMaps) {
        this.directCallees = directCallees;
        this.isDirectCallToRuntime = isDirectCallToRuntime;
        this.numberOfIndirectCalls = numberOfIndirectCalls;
        this.numberOfSafepoints = numberOfSafepoints;
        this.stopPositions = stopPositions;
        this.referenceMaps = referenceMaps;
        this.bytecodeStopsIterator = bytecodeStopsIterator;
    }

    public int numberOfDirectCalls() {
        return directCallees == null ? 0 : directCallees.length;
    }

    /**
     * A facility for aggregating {@link StopType stop} related information when translating a method with a
     * {@linkplain BytecodeToTargetTranslator template based JIT compiler}.
     *
     * @author Doug Simon
     */
    public static class StopsBuilder {

        private final int[] stopTypeCount;
        private Stop[] stops;
        private int uniqueBytecodePositions = 1;
        private int count;

        public StopsBuilder(int initialCapacity) {
            stops = new Stop[initialCapacity];
            stopTypeCount = new int[StopType.VALUES.length()];
        }

        private void ensureCapacity(int minCapacity) {
            final int oldCapacity = stops.length;
            if (minCapacity > oldCapacity) {
                int newCapacity = ((oldCapacity * 3) >> 1) + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                // minCapacity is usually close to size, so this is a win:
                stops = Arrays.copyOf(stops, newCapacity);
            }

        }

        private void addNoCheck(Stop stop) {
            stops[count] = stop;
            stopTypeCount[stop.type().ordinal()]++;
            final int bytecodePosition = stop.bytecodePosition;
            if (count > 0) {
                final int previousBytecodePosition = stops[count - 1].bytecodePosition;
                if (bytecodePosition > previousBytecodePosition) {
                    uniqueBytecodePositions++;
                } else {
                    assert bytecodePosition == previousBytecodePosition : "Stops must be accumulated in bytecode order";
                }
            }
            count++;
        }

        public void add(Stop stop) {
            ensureCapacity(count + 1);
            addNoCheck(stop);
        }

        public void add(CompiledBytecodeTemplate template, int targetCodePosition, int bytecodePosition) {
            final TargetMethod targetMethod = template.targetMethod;
            if (targetMethod.numberOfStopPositions() == 0) {
                return;
            }
            final int numberOfDirectCalls = targetMethod.numberOfDirectCalls();
            final int numberOfIndirectCalls = targetMethod.numberOfIndirectCalls();
            final int numberOfSafepoints = targetMethod.numberOfSafepoints();

            ensureCapacity(count + numberOfDirectCalls + numberOfIndirectCalls + numberOfSafepoints);

            for (int i = 0; i < numberOfDirectCalls; i++) {
                final int stopPosition = targetCodePosition + DIRECT_CALL.stopPosition(targetMethod, i);
                addNoCheck(new TemplateDirectCall(stopPosition, bytecodePosition, targetMethod, i));
            }
            for (int i = 0; i < numberOfIndirectCalls; i++) {
                final int stopPosition = targetCodePosition + INDIRECT_CALL.stopPosition(targetMethod, i);
                addNoCheck(new TemplateIndirectCall(stopPosition, bytecodePosition, targetMethod, i));
            }
            for (int i = 0; i < numberOfSafepoints; i++) {
                final int stopPosition = targetCodePosition + SAFEPOINT.stopPosition(targetMethod, i);
                addNoCheck(new TemplateSafepoint(stopPosition, bytecodePosition, targetMethod, i));
            }
        }

        public Stops pack(int frameReferenceMapSize, int registerReferenceMapSize, int firstTemplateSlot) {
            final int numberOfDirectCalls = stopTypeCount[DIRECT_CALL.ordinal()];
            final int numberOfIndirectCalls = stopTypeCount[INDIRECT_CALL.ordinal()];
            final int numberOfSafepoints = stopTypeCount[SAFEPOINT.ordinal()];
            final int numberOfStopPositions = count;
            final int numberOfCalls = numberOfDirectCalls + numberOfIndirectCalls;
            assert numberOfStopPositions == numberOfCalls + numberOfSafepoints;

            int[] stopPositions = null;
            ClassMethodActor[] directCallees = null;
            ByteArrayBitMap isDirectCallToRuntimeMap = null;
            byte[] referenceMaps = null;
            BytecodeStopsIterator bytecodeStopsIterator = null;

            if (numberOfStopPositions > 0) {
                assert frameReferenceMapSize > 0 || numberOfSafepoints > 0;
                referenceMaps = new byte[(numberOfStopPositions * frameReferenceMapSize) + (numberOfSafepoints  * registerReferenceMapSize)];
                stopPositions = new int[numberOfStopPositions];
                final int[] bytecodeStopsMap = new int[numberOfStopPositions + uniqueBytecodePositions];
                int bytecodeStopsMapIndex = 0;
                if (numberOfDirectCalls > 0) {
                    isDirectCallToRuntimeMap = new ByteArrayBitMap(numberOfDirectCalls);
                    directCallees = new ClassMethodActor[numberOfDirectCalls];
                }

                final int firstSafepointRegisterReferenceMapIndex = numberOfStopPositions * frameReferenceMapSize;

                int directCallIndex = 0;
                int indirectCallIndex = numberOfDirectCalls;
                int safepointIndex = numberOfCalls;

                final ByteArrayBitMap bitMap = new ByteArrayBitMap(referenceMaps, 0, frameReferenceMapSize);
                int lastBytecodePosition = -1;
                for (int i = 0; i != numberOfStopPositions; ++i) {
                    final Stop stop = stops[i];
                    final int stopIndex;
                    boolean isDirectCallToRuntime = false;
                    switch (stop.type()) {
                        case DIRECT_CALL: {
                            directCallees[directCallIndex] = stop.directCallee();
                            assert directCallees[directCallIndex] != null;
                            if (stop.isDirectRuntimeCall()) {
                                isDirectCallToRuntime = true;
                            }
                            stopIndex = directCallIndex++;
                            break;
                        }
                        case INDIRECT_CALL: {
                            stopIndex = indirectCallIndex++;
                            break;
                        }
                        case SAFEPOINT: {
                            stopIndex = safepointIndex++;
                            bitMap.setOffset(firstSafepointRegisterReferenceMapIndex + ((stopIndex - numberOfCalls) * registerReferenceMapSize));
                            bitMap.setSize(registerReferenceMapSize);
                            stop.initializeRegisterReferenceMap(bitMap);
                            break;
                        }
                        default:
                            throw ProgramError.unknownCase();
                    }

                    final int bytecodePosition = stop.bytecodePosition;
                    if (lastBytecodePosition != bytecodePosition) {
                        bytecodeStopsMap[bytecodeStopsMapIndex++] = bytecodePosition | BytecodeStopsIterator.BCP_BIT;
                        lastBytecodePosition = bytecodePosition;
                    }
                    if (isDirectCallToRuntime) {
                        isDirectCallToRuntimeMap.set(stopIndex);
                        bytecodeStopsMap[bytecodeStopsMapIndex++] = stopIndex | BytecodeStopsIterator.DIRECT_RUNTIME_CALL_BIT;
                    } else {
                        bytecodeStopsMap[bytecodeStopsMapIndex++] = stopIndex;
                    }
                    stopPositions[stopIndex] = stop.position;
                    bitMap.setSize(frameReferenceMapSize);
                    bitMap.setOffset(stopIndex * frameReferenceMapSize);
                    stop.initializeStackReferenceMap(bitMap, firstTemplateSlot);
                }
                assert directCallIndex == numberOfDirectCalls;
                assert indirectCallIndex == numberOfDirectCalls + numberOfIndirectCalls;
                assert safepointIndex == numberOfStopPositions;
                assert bytecodeStopsMapIndex == bytecodeStopsMap.length;
                bytecodeStopsIterator = new BytecodeStopsIterator(bytecodeStopsMap);
            }
            return new Stops(directCallees, isDirectCallToRuntimeMap, numberOfIndirectCalls, numberOfSafepoints, stopPositions, bytecodeStopsIterator, referenceMaps);
        }
    }
}
