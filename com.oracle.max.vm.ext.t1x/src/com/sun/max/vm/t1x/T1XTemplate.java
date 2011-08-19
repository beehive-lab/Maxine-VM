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

import static com.sun.max.vm.compiler.target.Safepoints.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.Safepoints.Attr;

/**
 * A T1X template is a piece of machine code (and its associated metadata) that
 * is used by the T1X compiler to quickly translate a bytecode instruction
 * to native code.
 */
public class T1XTemplate {

    public static class T1XSafepoint {
        public static final int NO_BSM_INDEX = Integer.MIN_VALUE;

        public T1XSafepoint() {
        }

        public T1XSafepoint(int safepoint, int bci) {
            this.safepoint = safepoint;
            this.bci = bci;
        }

        int safepoint;

        public int pos() {
            return Safepoints.pos(safepoint);
        }

        public int causePos() {
            return Safepoints.causePos(safepoint);
        }

        public int attrs() {
            return safepoint & ATTRS_MASK;
        }

        public boolean isSet(Attr a) {
            return a.isSet(safepoint);
        }

        public boolean isCall() {
            return isSet(DIRECT_CALL) || isSet(INDIRECT_CALL);
        }

        /**
         * Bytecode index of safepoint. This is {@code -1} for a template.
         */
        int bci;
        ClassMethodActor callee;
        CiBitMap frameRefMap;
        CiBitMap regRefMap;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Attr f : Safepoints.ALL_ATTRS) {
                if (f.isSet(safepoint)) {
                    sb.append(f.name).append(' ');
                }
            }
            sb.append("@ ").
                append(pos()).
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

    private static final T1XSafepoint[] NO_SAFEPOINTS = {};

    public final ClassMethodActor method;
    public final T1XTemplateTag tag;
    public final byte[] code;
    public final T1XSafepoint templateCall;
    public final T1XSafepoint[] safepoints;

    @Override
    public String toString() {
        return String.valueOf(tag) + " [" + method + "]";
    }

    static final class SafepointArray {
        T1XSafepoint[] data = new T1XSafepoint[10];
        int size;

        public void add(T1XSafepoint safepoint) {
            ensureCapacity(data.length + 1);
            data[size++] = safepoint;
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

        public T1XSafepoint make(int index) {
            ensureCapacity(index + 1);
            T1XSafepoint safepoint = data[index];
            if (safepoint == null) {
                safepoint = new T1XSafepoint();
                data[index] = safepoint;
            }
            return safepoint;
        }

        public T1XSafepoint makeNext() {
            return make(size++);
        }

        public void clear() {
            size = 0;
        }

        public T1XSafepoint get(int i) {
            return data[i];
        }
    }

    public static class SafepointsBuilder {
        SafepointArray safepointsArray;
        Safepoints safepoints;
        int directCalls;
        int bcisWithSafepoints;
        int lastBci;

        public Object[] directCallees;
        public byte[] refMaps;
        public BytecodeSafepointsIterator bytecodeSafepointsIterator;

        public SafepointsBuilder() {
            reset(true);
        }

        void reset(boolean hard) {
            directCalls = 0;
            bcisWithSafepoints = 0;
            lastBci = -1;
            if (hard) {
                safepointsArray = new SafepointArray();
            } else {
                safepointsArray.clear();
            }
        }

        void reserveInBSM(T1XSafepoint safepoint) {
            if (safepoint.bci >= 0) {
                if (lastBci != safepoint.bci) {
                    lastBci = safepoint.bci;
                    bcisWithSafepoints++;
                }
            }
        }

        public void addSafepoint(int bci, int pos) {
            T1XSafepoint dst = safepointsArray.makeNext();
            dst.bci = bci;
            dst.safepoint = Safepoints.make(pos);
            dst.callee = null;

            // No GC maps needed: the template slots are dead for the remainder of the template
            dst.regRefMap = null;
            dst.frameRefMap = null;
            reserveInBSM(dst);
        }

        public void add(T1XTemplate template, int pos, int bci, ClassMethodActor directBytecodeCallee) {
            for (T1XSafepoint src : template.safepoints) {
                T1XSafepoint dst = safepointsArray.makeNext();
                dst.bci = bci;
                dst.safepoint = make(pos + src.pos(), pos + src.causePos(), src.attrs());
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
                    assert !src.isSet(DIRECT_CALL);
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
            final int safepointsCount = safepointsArray.size + adapterCount;

            safepoints = null;
            directCallees = null;
            refMaps = null;
            bytecodeSafepointsIterator = null;

            if (safepointsCount > 0) {
                assert frameRefMapSize > 0;
                int refMapSize = frameRefMapSize + regRefMapSize;
                refMaps = new byte[safepointsCount * refMapSize];
                int[] safepoints = new int[safepointsCount];
                // An empty method with no method profile only has an adapter and no safepoints
                int[] bsm = new int[bcisWithSafepoints * 2];
                int firstSafepointIndexWithBCI = -1;
                int bsmIndex = -1;
                if (directCalls > 0) {
                    directCallees = new Object[directCalls];
                }

                final ByteArrayBitMap bitMap = new ByteArrayBitMap(refMaps, 0, 0);

                int safepointIndex = 0;
                int dcIndex = 0;
                if (adapter != null) {
                    directCallees[dcIndex++] = adapter;
                    int callPos = adapter.callOffsetInPrologue();
                    int safepointPos = safepointPosForCall(callPos, adapter.callSizeInPrologue());
                    safepoints[safepointIndex] = Safepoints.make(safepointPos, callPos, DIRECT_CALL);
                    safepointIndex++;
                }

                for (int i = 0; i < safepointsArray.size; i++) {
                    T1XSafepoint t1xSafepoint = safepointsArray.get(i);
                    safepoints[safepointIndex] = t1xSafepoint.safepoint;
                    if (t1xSafepoint.bci >= 0) {
                        if (bsmIndex == -1) {
                            bsmIndex = 0;
                            firstSafepointIndexWithBCI = safepointIndex;
                            bsm[bsmIndex] = t1xSafepoint.bci;
                        } else if (bsm[bsmIndex] != t1xSafepoint.bci) {
                            int prev = bsm[bsmIndex];
                            assert prev < t1xSafepoint.bci;
                            bsmIndex += 2;
                            assert bsmIndex + 1 < bsm.length;
                            bsm[bsmIndex] = t1xSafepoint.bci;
                        }
                        bsm[bsmIndex + 1]++;
                    }

                    if (t1xSafepoint.isSet(DIRECT_CALL)) {
                        directCallees[dcIndex++] = t1xSafepoint.callee;
                    }

                    if (t1xSafepoint.frameRefMap != null) {
                        bitMap.setOffset(safepointIndex * refMapSize);
                        bitMap.setSize(frameRefMapSize);
                        for (int bit = t1xSafepoint.frameRefMap.nextSetBit(0); bit >= 0; bit = t1xSafepoint.frameRefMap.nextSetBit(bit + 1)) {
                            bitMap.set(bit + firstTemplateSlot);
                        }
                    }
                    if (t1xSafepoint.regRefMap != null) {
                        bitMap.setOffset((safepointIndex * refMapSize) + frameRefMapSize);
                        bitMap.setSize(regRefMapSize);
                        for (int bit = t1xSafepoint.regRefMap.nextSetBit(0); bit >= 0; bit = t1xSafepoint.regRefMap.nextSetBit(bit + 1)) {
                            bitMap.set(bit);
                        }
                    }
                    safepointIndex++;
                }

                this.safepoints = new Safepoints(safepoints);
                bytecodeSafepointsIterator = new BytecodeSafepointsIterator(bsm, firstSafepointIndexWithBCI);
            } else {
                safepoints = Safepoints.NO_SAFEPOINTS;
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
        int nSafepoints = source.safepoints().size();

        if (nSafepoints == 0) {
            safepoints = NO_SAFEPOINTS;
            templateCall = null;
        } else {
            safepoints = new T1XSafepoint[nSafepoints];
            T1XSafepoint templateCall = null;

            Safepoints sourceSafepoints = source.safepoints();

            int dcIndex = 0;
            for (int safepointIndex = 0; safepointIndex < sourceSafepoints.size(); safepointIndex++) {
                int safepoint = sourceSafepoints.safepointAt(safepointIndex);
                T1XSafepoint t1xSafepoint = new T1XSafepoint(safepoint, -1);
                t1xSafepoint.callee = null;
                t1xSafepoint.frameRefMap = nullIfEmpty(source.debugInfo().frameRefMapAt(safepointIndex));
                t1xSafepoint.regRefMap = nullIfEmpty(source.debugInfo().regRefMapAt(safepointIndex));
                if (sourceSafepoints.isSetAt(DIRECT_CALL, safepointIndex)) {
                    if (sourceSafepoints.isSetAt(TEMPLATE_CALL, safepointIndex)) {
                        assert templateCall == null : "template can have at most one TEMPLATE_CALL";
                        templateCall = t1xSafepoint;
                    } else {
                        t1xSafepoint.callee = (ClassMethodActor) source.directCallees()[dcIndex];
                        assert t1xSafepoint.callee != null;
                    }
                    dcIndex++;
                }
                safepoints[safepointIndex] = t1xSafepoint;
            }

            this.templateCall = templateCall;
        }
    }
}
