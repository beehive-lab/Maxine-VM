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

import static com.sun.max.vm.compiler.target.Stops.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.Stops.Attr;

/**
 * A T1X template is a piece of machine code (and its associated metadata) that
 * is used by the T1X compiler to quickly translate a bytecode instruction
 * to native code.
 */
public class T1XTemplate {

    public static class T1XStop {
        public static final int NO_BSM_INDEX = Integer.MIN_VALUE;

        public T1XStop() {
        }

        public T1XStop(int stop, int bci) {
            this.stop = stop;
            this.bci = bci;
        }

        int stop;

        public int pos() {
            return stop & POS_MASK;
        }

        public int causePos() {
            return Stops.causePos(stop);
        }

        public int attrs() {
            return stop & ATTRS_MASK;
        }

        public boolean isSet(Attr a) {
            return a.isSet(stop);
        }

        public boolean isCall() {
            return isSet(DIRECT_CALL) || isSet(INDIRECT_CALL);
        }

        /**
         * Bytecode index of stop. This is {@code -1} for a template.
         */
        int bci;
        ClassMethodActor callee;
        CiBitMap frameRefMap;
        CiBitMap regRefMap;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Attr f : Stops.ALL_ATTRS) {
                if (f.isSet(stop)) {
                    sb.append(f.name).append(' ');
                }
            }
            sb.append("@ ").
                append(Stops.POS_MASK & stop).
                append(" [bci: ").
                append(bci);
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
    public final T1XStop templateCall;
    public final T1XStop[] stops;

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
        StopArray stopsArray;
        Stops stops;
        int directCalls;
        int bcisWithStops;
        int lastBci;

        public Object[] directCallees;
        public byte[] refMaps;
        public BytecodeStopsIterator bytecodeStopsIterator;

        public StopsBuilder() {
            reset(true);
        }

        void reset(boolean hard) {
            directCalls = 0;
            bcisWithStops = 0;
            lastBci = -1;
            if (hard) {
                stopsArray = new StopArray();
            } else {
                stopsArray.clear();
            }
        }

        void reserveInBSM(T1XStop stop) {
            if (stop.bci >= 0) {
                if (lastBci != stop.bci) {
                    lastBci = stop.bci;
                    bcisWithStops++;
                }
            }
        }

        public void addSafepoint(int bci, int pos) {
            T1XStop dst = stopsArray.makeNext();
            dst.bci = bci;
            dst.stop = Stops.make(pos, pos, SAFEPOINT);
            dst.callee = null;

            // No GC maps needed: the template slots are dead for the remainder of the template
            dst.regRefMap = null;
            dst.frameRefMap = null;
            reserveInBSM(dst);
        }

        public void add(T1XTemplate template, int pos, int bci, ClassMethodActor directBytecodeCallee) {
            for (T1XStop src : template.stops) {
                T1XStop dst = stopsArray.makeNext();
                dst.bci = bci;
                dst.stop = make(pos + src.pos(), pos + src.causePos(), src.attrs());
                if (src.isSet(DIRECT_CALL)) {
                    // The decision as to whether ref-maps are used is made when the template is created
                    dst.frameRefMap = src.frameRefMap;
                    dst.regRefMap = src.regRefMap;
                    if (src == template.templateCall) {
                        assert directBytecodeCallee != null : "bytecode call in template must bound to a direct bytecode callee";
                        dst.callee = directBytecodeCallee;

                        // Make sure the direct bytecode callee is bound at most once
                        directBytecodeCallee = null;
                    } else {
                        dst.callee = src.callee;
                    }
                    directCalls++;
                } else {
                    assert src.isSet(SAFEPOINT) || src.isSet(INDIRECT_CALL);
                    dst.callee = null;
                    dst.frameRefMap = src.frameRefMap;
                    dst.regRefMap = src.regRefMap;
                }
                reserveInBSM(dst);
            }
        }

        public void pack(int frameRefMapSize, int regRefMapSize, int firstTemplateSlot, Adapter adapter) {
            final int adapterCount = adapter == null ? 0 : 1;
            if (adapter != null) {
                directCalls++;
            }
            final int stopsCount = stopsArray.size + adapterCount;

            stops = null;
            directCallees = null;
            refMaps = null;
            bytecodeStopsIterator = null;

            if (stopsCount > 0) {
                assert frameRefMapSize > 0;
                int refMapSize = frameRefMapSize + regRefMapSize;
                refMaps = new byte[stopsCount * refMapSize];
                int[] stops = new int[stopsCount];
                // An empty method with no method profile only has an adapter and no stops
                int[] bsm = new int[bcisWithStops * 2];
                int firstStopIndexWithBCI = -1;
                int bsmIndex = -1;
                if (directCalls > 0) {
                    directCallees = new Object[directCalls];
                }

                final ByteArrayBitMap bitMap = new ByteArrayBitMap(refMaps, 0, 0);

                int stopIndex = 0;
                int dcIndex = 0;
                if (adapter != null) {
                    directCallees[dcIndex++] = adapter;
                    int callPos = adapter.callOffsetInPrologue();
                    int stopPos = stopPosForCall(callPos, adapter.callSizeInPrologue());
                    stops[stopIndex] = Stops.make(stopPos, callPos, DIRECT_CALL);
                    stopIndex++;
                }

                for (int i = 0; i < stopsArray.size; i++) {
                    T1XStop t1xStop = stopsArray.get(i);
                    stops[stopIndex] = t1xStop.stop;
                    if (t1xStop.bci >= 0) {
                        if (bsmIndex == -1) {
                            bsmIndex = 0;
                            firstStopIndexWithBCI = stopIndex;
                            bsm[bsmIndex] = t1xStop.bci;
                        } else if (bsm[bsmIndex] != t1xStop.bci) {
                            int prev = bsm[bsmIndex];
                            assert prev < t1xStop.bci;
                            bsmIndex += 2;
                            assert bsmIndex + 1 < bsm.length;
                            bsm[bsmIndex] = t1xStop.bci;
                        }
                        bsm[bsmIndex + 1]++;
                    }

                    if (t1xStop.isSet(DIRECT_CALL)) {
                        directCallees[dcIndex++] = t1xStop.callee;
                    }

                    if (t1xStop.frameRefMap != null) {
                        bitMap.setOffset(stopIndex * refMapSize);
                        bitMap.setSize(frameRefMapSize);
                        for (int bit = t1xStop.frameRefMap.nextSetBit(0); bit >= 0; bit = t1xStop.frameRefMap.nextSetBit(bit + 1)) {
                            bitMap.set(bit + firstTemplateSlot);
                        }
                    }
                    if (t1xStop.regRefMap != null) {
                        bitMap.setOffset((stopIndex * refMapSize) + frameRefMapSize);
                        bitMap.setSize(regRefMapSize);
                        for (int bit = t1xStop.regRefMap.nextSetBit(0); bit >= 0; bit = t1xStop.regRefMap.nextSetBit(bit + 1)) {
                            bitMap.set(bit);
                        }
                    }
                    stopIndex++;
                }

                this.stops = new Stops(stops);
                bytecodeStopsIterator = new BytecodeStopsIterator(bsm, firstStopIndexWithBCI);
            } else {
                stops = Stops.NO_STOPS;
                directCallees = TargetMethod.NO_DIRECT_CALLEES;
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
        int nStops = source.stops().length();

        if (nStops == 0) {
            stops = NO_STOPS;
            templateCall = null;
        } else {
            stops = new T1XStop[nStops];
            T1XStop templateCall = null;

            Stops sourceStops = source.stops();

            int dcIndex = 0;
            for (int stopIndex = 0; stopIndex < sourceStops.length(); stopIndex++) {
                int stop = sourceStops.stopAt(stopIndex);
                T1XStop t1xStop = new T1XStop(stop, -1);
                t1xStop.callee = null;
                t1xStop.frameRefMap = nullIfEmpty(source.debugInfo().frameRefMapAt(stopIndex));
                t1xStop.regRefMap = nullIfEmpty(source.debugInfo().regRefMapAt(stopIndex));
                if (sourceStops.isSetAt(DIRECT_CALL, stopIndex)) {
                    if (sourceStops.isSetAt(TEMPLATE_CALL, stopIndex)) {
                        assert templateCall == null : "template can have at most one TEMPLATE_CALL";
                        templateCall = t1xStop;
                    } else {
                        t1xStop.callee = (ClassMethodActor) source.directCallees()[dcIndex];
                        assert t1xStop.callee != null;
                    }
                    dcIndex++;
                }
                stops[stopIndex] = t1xStop;
            }

            this.templateCall = templateCall;
        }
    }
}
