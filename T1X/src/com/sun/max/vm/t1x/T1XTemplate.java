/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import static com.sun.max.vm.t1x.T1XTemplate.T1XStop.Flag.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.c1x.*;

/**
 * A T1X template is a piece of machine code (and its associated metadata) that
 * is used by the T1X compiler to quickly translate a bytecode instruction
 * to native code.
 */
public class T1XTemplate {

    public static class T1XStop {
        public enum Flag {
            BackwardBranch,
            Safepoint,
            DirectCall,
            IndirectCall,
            InTemplate;

            public final int mask = 1 << ordinal();
        }

        public static final int NO_BSM_INDEX = Integer.MIN_VALUE;

        public T1XStop() {
        }

        public T1XStop(int flags, int pos, int bci) {
            this.flags = flags;
            this.pos = pos;
            this.bci = bci;
        }
        int flags;
        int pos;

        /**
         * Bytecode index of stop. This is {@code -1} for a template.
         */
        int bci;
        ClassMethodActor callee;
        CiBitMap frameRefMap;
        CiBitMap regRefMap;

        /**
         * Index of this stop in the bytecode stops map.
         */
        int bsmIndex;

        int bsmIndex() {
            return bsmIndex < 0 ? -bsmIndex : bsmIndex;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Flag f : Flag.values()) {
                if ((flags & f.mask) != 0) {
                    sb.append(f).append(' ');
                }
            }
            sb.append("@ ").
                append(pos).
                append(" [bci: ").
                append(bci).
                append(", bsmIndex: ").
                append(bsmIndex == NO_BSM_INDEX ? "<none>" : String.valueOf(bsmIndex()));
            if (frameRefMap != null) {
                sb.append(", frameRefMap:").append(frameRefMap);
            }
            if (regRefMap != null) {
                sb.append(", regRefMap:").append(regRefMap);
            }
            return sb.append("]").toString();
        }
    }

    private static final T1XStop[] NO_STOPS = {};

    public final ClassMethodActor method;
    public final T1XTemplateTag tag;
    public final byte[] code;
    public final T1XStop bytecodeCall;
    public final T1XStop[] directCalls;
    public final T1XStop[] indirectCalls;
    public final T1XStop[] safepoints;
    public final int numberOfStops;

    @Override
    public String toString() {
        return String.valueOf(tag) + " [" + method + "]";
    }

    static final class StopArray {
        T1XStop[] data = new T1XStop[10];
        int size;

        public void add(T1XStop stop) {
            ensureCapacity(data.length + 1);
            data[size++] = stop;
        }

        void ensureCapacity(int minCapacity) {
            if (minCapacity > data.length) {
                int newCapacity = (size * 3) / 2 + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                data = Arrays.copyOf(data, newCapacity);
            }
        }

        public T1XStop make(int index) {
            ensureCapacity(index + 1);
            T1XStop stop = data[index];
            if (stop == null) {
                stop = new T1XStop();
                data[index] = stop;
            }
            return stop;
        }

        public T1XStop makeNext() {
            return make(size++);
        }

        public void clear() {
            size = 0;
        }

        public T1XStop get(int i) {
            return data[i];
        }
    }

    public static class StopsBuilder {
        StopArray directCalls;
        StopArray indirectCalls;
        StopArray safepoints;

        T1XStop last;

        private int bciWithStopCount;

        public int[] stopPositions;
        public ClassMethodActor[] directCallees;
        public byte[] refMaps;
        public CiBitMap isDirectCallToRuntime;
        public BytecodeStopsIterator bytecodeStopsIterator;

        public StopsBuilder() {
            reset(true);
        }

        void reset(boolean hard) {
            last = null;
            bciWithStopCount = 0;
            if (hard) {
                directCalls = new StopArray();
                indirectCalls = new StopArray();
                safepoints = new StopArray();
            } else {
                directCalls.clear();
                indirectCalls.clear();
                safepoints.clear();
            }
        }

        void reserveInBSM(T1XStop stop) {
            if (stop.bci >= 0) {
                if (last != null) {
                    if (last.bci == stop.bci) {
                        stop.bsmIndex = last.bsmIndex() + 1;
                    } else {
                        stop.bsmIndex = -(last.bsmIndex() + 2);
                    }
                } else {
                    stop.bsmIndex = -1;
                }
                last = stop;
            } else {
                stop.bsmIndex = T1XStop.NO_BSM_INDEX;
            }
        }

        int insertInBSM(int[] bsm, T1XStop stop) {
            if (stop.bsmIndex < 0 && stop.bsmIndex != T1XStop.NO_BSM_INDEX) {
                stop.bsmIndex = -stop.bsmIndex;
                bsm[stop.bsmIndex - 1] = stop.bci | BytecodeStopsIterator.BCP_BIT;
            }
            return stop.bsmIndex;
        }

        public void addBytecodeBackwardBranch(int bci, int pos) {
            T1XStop dst = safepoints.makeNext();
            dst.bci = bci;
            dst.pos = pos;
            dst.callee = null;
            dst.flags = Safepoint.mask;

            // No GC maps needed: the template slots are dead for the remainder of the template
            dst.regRefMap = null;
            dst.frameRefMap = null;
            reserveInBSM(dst);
            bciWithStopCount++;
        }

        public void add(T1XTemplate template, int pos, int bci, ClassMethodActor directBytecodeCallee) {
            for (T1XStop src : template.directCalls) {
                T1XStop dst = directCalls.makeNext();
                dst.bci = bci;
                dst.pos = pos + src.pos;
                if (src == template.bytecodeCall) {
                    assert directBytecodeCallee != null : "bytecode call in template must bound to a direct bytecode callee";
                    dst.callee = directBytecodeCallee;
                    dst.flags = DirectCall.mask;

                    // No GC maps needed: the template slots are dead for the remainder of the template
                    dst.regRefMap = null;
                    dst.frameRefMap = null;

                    // Make sure the direct bytecode callee is bound at most once
                    directBytecodeCallee = null;
                } else {
                    dst.flags = src.flags;
                    dst.callee = src.callee;
                    dst.frameRefMap = src.frameRefMap;
                    dst.regRefMap = null;
                }
                reserveInBSM(dst);
            }
            for (T1XStop src : template.indirectCalls) {
                T1XStop dst = indirectCalls.makeNext();
                dst.bci = bci;
                dst.pos = pos + src.pos;
                dst.flags = src.flags;
                dst.callee = null;
                dst.frameRefMap = src.frameRefMap;
                dst.regRefMap = null;
                reserveInBSM(dst);
            }
            for (T1XStop src : template.safepoints) {
                T1XStop dst = safepoints.makeNext();
                dst.bci = bci;
                dst.pos = pos + src.pos;
                dst.flags = src.flags;
                dst.callee = null;
                dst.frameRefMap = src.frameRefMap;
                dst.regRefMap = src.regRefMap;
                reserveInBSM(dst);
            }
            if (bci >= 0) {
                bciWithStopCount++;
            }
        }

        public void pack(int frameRefMapSize, int regRefMapSize, int firstTemplateSlot) {
            final int numberOfDirectCalls = directCalls.size;
            final int numberOfIndirectCalls = indirectCalls.size;
            final int numberOfSafepoints = safepoints.size;
            final int numberOfStopPositions = numberOfDirectCalls + numberOfIndirectCalls + numberOfSafepoints;
            final int numberOfCalls = numberOfDirectCalls + numberOfIndirectCalls;
            assert numberOfStopPositions == numberOfCalls + numberOfSafepoints;

            stopPositions = null;
            directCallees = null;
            isDirectCallToRuntime = null;
            refMaps = null;
            bytecodeStopsIterator = null;

            if (numberOfStopPositions > 0) {
                assert frameRefMapSize > 0 || numberOfSafepoints > 0;
                int refMapSize = frameRefMapSize + regRefMapSize;
                refMaps = new byte[numberOfStopPositions * refMapSize];
                stopPositions = new int[numberOfStopPositions];
                int[] bsm = new int[last.bsmIndex() + 1];
                last = null;
                if (numberOfDirectCalls > 0) {
                    isDirectCallToRuntime = new CiBitMap(numberOfDirectCalls);
                    directCallees = new ClassMethodActor[numberOfDirectCalls];
                }

                final ByteArrayBitMap bitMap = new ByteArrayBitMap(refMaps, 0, 0);

                int stopIndex = 0;
                for (int i = 0; i < numberOfDirectCalls; ++i, ++stopIndex) {
                    T1XStop stop = directCalls.get(i);
                    assert stop.regRefMap == null;
                    directCallees[i] = stop.callee;

                    int bsmIndex = insertInBSM(bsm, stop);

                    if ((stop.flags & InTemplate.mask) != 0) {
                        isDirectCallToRuntime.set(stopIndex);
                        if (bsmIndex >= 0) {
                            bsm[bsmIndex] = stopIndex | BytecodeStopsIterator.DIRECT_RUNTIME_CALL_BIT;
                        }

                        if (stop.frameRefMap != null) {
                            bitMap.setOffset(stopIndex * refMapSize);
                            bitMap.setSize(frameRefMapSize);
                            for (int bit = stop.frameRefMap.nextSetBit(0); bit >= 0; bit = stop.frameRefMap.nextSetBit(bit + 1)) {
                                bitMap.set(bit + firstTemplateSlot);
                            }
                        }

                    } else {
                        assert stop.frameRefMap == null;
                        assert bsmIndex >= 0;
                        bsm[bsmIndex] = stopIndex;
                    }
                    stopPositions[stopIndex] = stop.pos;
                }

                for (int i = 0; i < numberOfIndirectCalls; ++i, ++stopIndex) {
                    T1XStop stop = indirectCalls.get(i);
                    assert stop.regRefMap == null;

                    int bsmIndex = insertInBSM(bsm, stop);

                    if ((stop.flags & InTemplate.mask) != 0) {
                        if (stop.frameRefMap != null) {
                            bitMap.setOffset(stopIndex * refMapSize);
                            bitMap.setSize(frameRefMapSize);
                            for (int bit = stop.frameRefMap.nextSetBit(0); bit >= 0; bit = stop.frameRefMap.nextSetBit(bit + 1)) {
                                bitMap.set(bit + firstTemplateSlot);
                            }
                        }

                    } else {
                        assert stop.frameRefMap == null;
                    }

                    if (bsmIndex >= 0) {
                        bsm[bsmIndex] = stopIndex;
                    }
                    stopPositions[stopIndex] = stop.pos;
                }

                for (int i = 0; i < numberOfSafepoints; ++i, ++stopIndex) {
                    T1XStop stop = safepoints.get(i);

                    int bsmIndex = insertInBSM(bsm, stop);

                    if ((stop.flags & InTemplate.mask) != 0) {
                        if (stop.frameRefMap != null) {
                            bitMap.setOffset(stopIndex * refMapSize);
                            bitMap.setSize(frameRefMapSize);
                            for (int bit = stop.frameRefMap.nextSetBit(0); bit >= 0; bit = stop.frameRefMap.nextSetBit(bit + 1)) {
                                bitMap.set(bit + firstTemplateSlot);
                            }
                        }
                        if (stop.regRefMap != null) {
                            bitMap.setOffset((stopIndex * refMapSize) + frameRefMapSize);
                            bitMap.setSize(regRefMapSize);
                            for (int bit = stop.regRefMap.nextSetBit(0); bit >= 0; bit = stop.regRefMap.nextSetBit(bit + 1)) {
                                bitMap.set(bit);
                            }
                        }

                    } else {
                        assert stop.frameRefMap == null;
                        assert stop.regRefMap == null;
                    }

                    if (bsmIndex >= 0) {
                        bsm[bsmIndex] = stopIndex;
                    }
                    stopPositions[stopIndex] = stop.pos;
                }
                bytecodeStopsIterator = new BytecodeStopsIterator(bsm);
            }
        }
    }

    @HOSTED_ONLY
    private static CiBitMap nullIfEmpty(CiBitMap bm) {
        return bm.cardinality() == 0 ? null : bm;
    }

    @HOSTED_ONLY
    public T1XTemplate(C1XTargetMethod source, T1XTemplateTag tag, ClassMethodActor method) {
        this.method = method;
        this.code = source.code();
        this.tag = tag;
        numberOfStops = source.numberOfStopPositions();
        if (source.numberOfStopPositions() == 0) {
            directCalls = NO_STOPS;
            indirectCalls = NO_STOPS;
            safepoints = NO_STOPS;
            bytecodeCall = null;
        } else {
            final int numberOfDirectCalls = source.numberOfDirectCalls();
            final int numberOfIndirectCalls = source.numberOfIndirectCalls();
            final int numberOfSafepoints = source.numberOfSafepoints();

            directCalls = new T1XStop[numberOfDirectCalls];
            indirectCalls = new T1XStop[numberOfIndirectCalls];
            safepoints = new T1XStop[numberOfSafepoints];
            T1XStop bytecodeCall = null;

            int frameRefMapSize = source.frameRefMapSize();
            int totalRefMapSize = source.totalRefMapSize();
            int regRefMapSize = C1XTargetMethod.regRefMapSize();

            for (int i = 0; i < numberOfDirectCalls; i++) {
                int stopIndex = i;
                if (source.directCallees()[i] == null) {
                    assert bytecodeCall == null : "template can have at most one TEMPLATE_CALL";
                    T1XStop stop = new T1XStop(DirectCall.mask, source.stopPosition(stopIndex), -1);
                    stop.callee = null;
                    // No GC maps needed: the template slots are dead for the remainder of the template
                    stop.frameRefMap = null;
                    stop.regRefMap = null;
                    bytecodeCall = stop;
                    directCalls[i] = stop;
                } else {
                    T1XStop stop = new T1XStop(DirectCall.mask | InTemplate.mask, source.stopPosition(stopIndex), -1);
                    stop.callee = (ClassMethodActor) source.directCallees()[i];
                    stop.frameRefMap = nullIfEmpty(new CiBitMap(source.referenceMaps(), stopIndex * totalRefMapSize, frameRefMapSize));
                    //stop.regRefMap = nullIfEmpty(CiBitMap(code.referenceMaps(), stopIndex * totalRefMapSize + frameRefMapSize, regRefMapSize));
                    directCalls[i] = stop;
                }
            }
            for (int i = 0; i < numberOfIndirectCalls; i++) {
                int stopIndex = numberOfDirectCalls + i;
                T1XStop stop = new T1XStop(IndirectCall.mask | InTemplate.mask, source.stopPosition(stopIndex), -1);
                stop.frameRefMap = nullIfEmpty(new CiBitMap(source.referenceMaps(), stopIndex * totalRefMapSize, frameRefMapSize));
                //stop.regRefMap = nullIfEmpty(new CiBitMap(code.referenceMaps(), stopIndex * totalRefMapSize + frameRefMapSize, regRefMapSize));
                indirectCalls[i] = stop;
            }
            for (int i = 0; i < numberOfSafepoints; i++) {
                int stopIndex = numberOfDirectCalls + numberOfIndirectCalls + i;
                T1XStop stop = new T1XStop(Safepoint.mask | InTemplate.mask, source.stopPosition(stopIndex), -1);
                stop.frameRefMap = nullIfEmpty(new CiBitMap(source.referenceMaps(), stopIndex * totalRefMapSize, frameRefMapSize));
                stop.regRefMap = nullIfEmpty(new CiBitMap(source.referenceMaps(), stopIndex * totalRefMapSize + frameRefMapSize, regRefMapSize));
                safepoints[i] = stop;
            }

            this.bytecodeCall = bytecodeCall;
        }
    }
}
