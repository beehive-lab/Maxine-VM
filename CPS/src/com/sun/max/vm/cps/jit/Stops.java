/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.jit;

import static com.sun.max.vm.compiler.target.StopType.*;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.jit.Stop.TemplateDirectCall;
import com.sun.max.vm.cps.jit.Stop.TemplateIndirectCall;
import com.sun.max.vm.cps.jit.Stop.TemplateSafepoint;

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
            stopTypeCount = new int[StopType.VALUES.size()];
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

        public void add(TargetMethod template, int targetCodePosition, int bytecodePosition) {
            if (template.numberOfStopPositions() == 0) {
                return;
            }
            final int numberOfDirectCalls = template.numberOfDirectCalls();
            final int numberOfIndirectCalls = template.numberOfIndirectCalls();
            final int numberOfSafepoints = template.numberOfSafepoints();

            ensureCapacity(count + numberOfDirectCalls + numberOfIndirectCalls + numberOfSafepoints);

            for (int i = 0; i < numberOfDirectCalls; i++) {
                final int stopPosition = targetCodePosition + DIRECT_CALL.stopPosition(template, i);
                addNoCheck(new TemplateDirectCall(stopPosition, bytecodePosition, template, i));
            }
            for (int i = 0; i < numberOfIndirectCalls; i++) {
                final int stopPosition = targetCodePosition + INDIRECT_CALL.stopPosition(template, i);
                addNoCheck(new TemplateIndirectCall(stopPosition, bytecodePosition, template, i));
            }
            for (int i = 0; i < numberOfSafepoints; i++) {
                final int stopPosition = targetCodePosition + SAFEPOINT.stopPosition(template, i);
                addNoCheck(new TemplateSafepoint(stopPosition, bytecodePosition, template, i));
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
