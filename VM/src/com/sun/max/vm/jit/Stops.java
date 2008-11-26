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

    public final int[] _stopPositions;
    public final ClassMethodActor[] _directCallees;
    public final int _numberOfIndirectCalls;
    public final int _numberOfSafepoints;
    public final byte[] _referenceMaps;
    public final ByteArrayBitMap _isDirectCallToRuntime;

    Stops(ClassMethodActor[] directCallees,
           ByteArrayBitMap isDirectCallToRuntime,
           int numberOfIndirectCalls,
           int numberOfSafepoints,
           int[] stopPositions,
           byte[] referenceMaps) {
        _directCallees = directCallees;
        _isDirectCallToRuntime = isDirectCallToRuntime;
        _numberOfIndirectCalls = numberOfIndirectCalls;
        _numberOfSafepoints = numberOfSafepoints;
        _stopPositions = stopPositions;
        _referenceMaps = referenceMaps;
    }

    public int numberOfDirectCalls() {
        return _directCallees == null ? 0 : _directCallees.length;
    }

    /**
     * A facility for aggregating {@link StopType stop} related information when translating a method with a
     * {@linkplain BytecodeToTargetTranslator template based JIT compiler}.
     *
     * @author Doug Simon
     */
    public static class StopsBuilder {

        private final int[] _stopTypeCount;
        private Stop[] _stops;
        private int _count;

        public StopsBuilder(int initialCapacity) {
            _stops = new Stop[initialCapacity];
            _stopTypeCount = new int[StopType.VALUES.length()];
        }

        private void ensureCapacity(int minCapacity) {
            final int oldCapacity = _stops.length;
            if (minCapacity > oldCapacity) {
                int newCapacity = (oldCapacity * 3) / 2 + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                // minCapacity is usually close to size, so this is a win:
                _stops = Arrays.copyOf(_stops, newCapacity);
            }

        }

        private void addNoCheck(Stop stop) {
            _stops[_count++] = stop;
            _stopTypeCount[stop.type().ordinal()]++;
        }

        public void add(Stop stop) {
            ensureCapacity(_count + 1);
            addNoCheck(stop);
        }

        public void add(CompiledBytecodeTemplate template, int targetCodePosition) {
            final TargetMethod targetMethod = template.targetMethod();
            if (targetMethod.numberOfStopPositions() == 0) {
                return;
            }
            final int numberOfDirectCalls = targetMethod.numberOfDirectCalls();
            final int numberOfIndirectCalls = targetMethod.numberOfIndirectCalls();
            final int numberOfSafepoints = targetMethod.numberOfSafepoints();

            ensureCapacity(_count + numberOfDirectCalls + numberOfDirectCalls + numberOfSafepoints);

            for (int i = 0; i < numberOfDirectCalls; i++) {
                final int stopPosition = targetCodePosition + DIRECT_CALL.stopPosition(targetMethod, i);
                addNoCheck(new TemplateDirectCall(stopPosition, targetMethod, i));
            }
            for (int i = 0; i < numberOfIndirectCalls; i++) {
                final int stopPosition = targetCodePosition + INDIRECT_CALL.stopPosition(targetMethod, i);
                addNoCheck(new TemplateIndirectCall(stopPosition, targetMethod, i));
            }
            for (int i = 0; i < numberOfSafepoints; i++) {
                final int stopPosition = targetCodePosition + SAFEPOINT.stopPosition(targetMethod, i);
                addNoCheck(new TemplateSafepoint(stopPosition, targetMethod, i));
            }
        }

        Stops pack(int frameReferenceMapSize, int registerReferenceMapSize, int firstTemplateSlot) {
            final int numberOfDirectCalls = _stopTypeCount[DIRECT_CALL.ordinal()];
            final int numberOfIndirectCalls = _stopTypeCount[INDIRECT_CALL.ordinal()];
            final int numberOfSafepoints = _stopTypeCount[SAFEPOINT.ordinal()];
            final int numberOfStopPositions = _count;
            final int numberOfCalls = numberOfDirectCalls + numberOfIndirectCalls;
            assert numberOfStopPositions == numberOfCalls + numberOfSafepoints;

            int[] stopPositions = null;
            ClassMethodActor[] directCallees = null;
            ByteArrayBitMap isDirectCallToRuntime = null;
            byte[] referenceMaps = null;

            if (numberOfStopPositions > 0) {
                assert frameReferenceMapSize > 0 || numberOfSafepoints > 0;
                referenceMaps = new byte[(numberOfStopPositions * frameReferenceMapSize) + (numberOfSafepoints  * registerReferenceMapSize)];
                stopPositions = new int[numberOfStopPositions];
                if (numberOfDirectCalls > 0) {
                    isDirectCallToRuntime = new ByteArrayBitMap(numberOfDirectCalls);
                    directCallees = new ClassMethodActor[numberOfDirectCalls];
                }

                final int firstSafepointRegisterReferenceMapIndex = numberOfStopPositions * frameReferenceMapSize;

                int directCallIndex = 0;
                int indirectCallIndex = numberOfDirectCalls;
                int safepointIndex = numberOfCalls;

                final ByteArrayBitMap bitMap = new ByteArrayBitMap(referenceMaps, 0, frameReferenceMapSize);
                for (int i = 0; i != numberOfStopPositions; ++i) {
                    final Stop stop = _stops[i];
                    final int stopIndex;
                    switch (stop.type()) {
                        case DIRECT_CALL: {
                            directCallees[directCallIndex] = stop.directCallee();
                            assert directCallees[directCallIndex] != null;
                            if (stop.isDirectRuntimeCall()) {
                                isDirectCallToRuntime.set(directCallIndex);
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
                    stopPositions[stopIndex] = stop._position;
                    bitMap.setSize(frameReferenceMapSize);
                    bitMap.setOffset(stopIndex * frameReferenceMapSize);
                    stop.initializeStackReferenceMap(bitMap, firstTemplateSlot);
                }
                assert directCallIndex == numberOfDirectCalls;
                assert indirectCallIndex == numberOfDirectCalls + numberOfIndirectCalls;
                assert safepointIndex == numberOfStopPositions;
            }
            return new Stops(directCallees, isDirectCallToRuntime, numberOfIndirectCalls, numberOfSafepoints, stopPositions, referenceMaps);
        }
    }
}
