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
package com.oracle.max.vm.ext.t1x;

import static com.oracle.max.vm.ext.t1x.T1X.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;

import java.lang.annotation.*;
import java.util.*;

import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.amd64.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.Entry;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.Safepoints.Attr;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

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
         * Bytecode index of this safepoint. This is {@code -1} for a template.
         */
        int bci;

        /**
         * The direct callee (if any) at this safepoint.
         */
        Object callee;

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

    /**
     * The source bytecode of this template.
     */
    public final ClassMethodActor method;

    /**
     * The template tag.
     */
    public final T1XTemplateTag tag;

    /**
     * This template's machine code.
     */
    public final byte[] code;

    /**
     * The safepoints in this template.
     */
    public final T1XSafepoint[] safepoints;

    /**
     * The object literals in this template or {@code null} if there are none.
     */
    public final Object[] objectLiterals;

    /**
     * Entry {@code i} indicates a position in {@code code} that must be {@linkplain T1XCompilation#fixup() patched}
     * to load entry {@code i} from {@link #objectLiterals}. The exact interpretation of the position is
     * platform dependent. For example, on AMD64, it is the displacement operand of a MOVQ instruction.
     */
    public final int[] objectLiteralDataPatches;

    /**
     * Describes the signature of a {@linkplain T1X_TEMPLATE template}
     * in terms of register mapping for the parameters and stack usage.
     */
    public final Sig sig;

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
        int lastBCI;
        int lastNonTemplateSafepointBCI;

        public Object[] directCallees;
        public byte[] refMaps;
        public BytecodeSafepointsIterator bytecodeSafepointsIterator;

        public SafepointsBuilder() {
            reset(true);
        }

        void reset(boolean hard) {
            directCalls = 0;
            bcisWithSafepoints = 0;
            lastBCI = -1;
            lastNonTemplateSafepointBCI = -1;
            if (hard) {
                safepointsArray = new SafepointArray();
            } else {
                safepointsArray.clear();
            }
        }

        void reserveInBSM(T1XSafepoint safepoint) {
            if (safepoint.bci >= 0) {
                if (lastBCI != safepoint.bci) {
                    lastBCI = safepoint.bci;
                    bcisWithSafepoints++;
                }
            }
        }

        /**
         * Adds a safepoint for non-template code. Such a safepoint must come
         * after all template code safepoints. This way we know that the
         * template slots are dead and so can be ignored in the gc maps.
         *
         * @param bci
         * @param safepoint
         * @param directCallee
         */
        public void addSafepoint(int bci, int safepoint, ClassMethodActor directCallee) {
            T1XSafepoint dst = safepointsArray.makeNext();
            dst.bci = bci;
            dst.safepoint = safepoint;
            dst.callee = directCallee;
            if (directCallee != null) {
                directCalls++;
            }

            // No GC maps needed: the template slots are dead for the remainder of the template
            dst.regRefMap = null;
            dst.frameRefMap = null;
            reserveInBSM(dst);
            lastNonTemplateSafepointBCI = bci;
        }

        public void add(T1XTemplate template, int pos, int bci) {
            assert bci == -1 || bci != lastNonTemplateSafepointBCI : "safepoints in template code must always precede non-template safepoints for any specific BCI";

            for (T1XSafepoint src : template.safepoints) {
                T1XSafepoint dst = safepointsArray.makeNext();
                dst.bci = bci;
                dst.safepoint = make(pos + src.pos(), pos + src.causePos(), src.attrs());
                if (src.isSet(DIRECT_CALL)) {
                    // The decision as to whether ref-maps are used is made when the template is created
                    dst.frameRefMap = src.frameRefMap;
                    dst.regRefMap = src.regRefMap;
                    dst.callee = src.callee;
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
            directCallees = TargetMethod.NO_DIRECT_CALLEES;
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
            }
        }
    }

    /**
     * Describes an argument or return value of a {@linkplain T1X_TEMPLATE template} method.
     */
    public static class Arg {
        /**
         * The kind of this arg.
         */
        public final Kind kind;

        public final String name;

        /**
         * The register in which this arg is passed.
         */
        public final CiRegister reg;

        /**
         * The operand stack index of the slot(s) holding this arg's value.
         * This will be -1 if this arg does not get its value from the operand stack.
         * @see Slot
         */
        public final int slot;

        public Arg(Kind kind, CiRegister reg, String name, int slot) {
            this.kind = kind;
            this.name = name;
            this.reg = reg;
            this.slot = slot;
        }

        /**
         * Determines if this arg gets its value from the operand stack.
         * If this arg represents the return value, then this method
         * determines if the result is written to the stack.
         */
        public boolean isStack() {
            return slot >= 0;
        }

        /**
         * Gets the number of stack slots holding this arg's value.
         * This will be 0 if this arg does not get its value from the stack.
         */
        public int stackSlots() {
            if (slot < 0) {
                return 0;
            } else {
                return kind.stackSlots;
            }
        }

        @Override
        public String toString() {
            return name + ':' + kind + "[reg=" + reg + ", slot=" + slot + "]";
        }
    }

    /**
     * Describes the signature of a {@linkplain T1X_TEMPLATE template}
     * in terms of register mapping for the parameters and stack usage.
     */
    public static class Sig {
        /**
         * The parameters of the template method.
         */
        public final Arg[] in;

        /**
         * The return value of the template method.
         */
        public final Arg out;

        /**
         * The net adjustment in terms of slots to the operand stack based on
         * the stack-based parameters and stack-based result of the template.
         */
        public final int stackDelta;

        /**
         * The number of {@link #in parameters} whose value comes from the operand stack.
         */
        public final int stackArgs;

        @HOSTED_ONLY
        public Sig(Arg[] in, Arg out) {
            this.in = in;
            this.out = out;

            int stackDelta = out.stackSlots();
            int stackArgs = 0;
            for (Arg a : in) {
                if (a.isStack()) {
                    stackDelta -= a.stackSlots();
                    stackArgs++;
                } else {
                    assert a.stackSlots() == 0;
                }
            }
            this.stackDelta = stackDelta;
            this.stackArgs = stackArgs;
        }
    }

    @HOSTED_ONLY
    private static CiBitMap nullIfEmpty(CiBitMap bm) {
        return bm.cardinality() == 0 ? null : bm;
    }

    @HOSTED_ONLY
    public T1XTemplate(MaxTargetMethod source, T1XTemplateTag tag, ClassMethodActor method) {
        this.method = method;
        this.code = source.code();
        this.tag = tag;
        int nSafepoints = source.safepoints().size();

        if (nSafepoints == 0) {
            safepoints = NO_SAFEPOINTS;
        } else {
            safepoints = new T1XSafepoint[nSafepoints];

            Safepoints sourceSafepoints = source.safepoints();

            int dcIndex = 0;
            for (int safepointIndex = 0; safepointIndex < sourceSafepoints.size(); safepointIndex++) {
                int safepoint = sourceSafepoints.safepointAt(safepointIndex);
                T1XSafepoint t1xSafepoint = new T1XSafepoint(safepoint, -1);
                t1xSafepoint.callee = null;
                t1xSafepoint.frameRefMap = nullIfEmpty(source.debugInfo().frameRefMapAt(safepointIndex));
                t1xSafepoint.regRefMap = nullIfEmpty(source.debugInfo().regRefMapAt(safepointIndex));
                if (sourceSafepoints.isSetAt(DIRECT_CALL, safepointIndex)) {
                    t1xSafepoint.callee = source.directCallees()[dcIndex];
                    assert t1xSafepoint.callee != null;
                    // The calling convention for stubs is not guaranteed to be compatible with T1X's
                    // calling convention. For example, some compiler stubs pass arguments and receive
                    // return values purely on the stack.
                    assert !(t1xSafepoint.callee instanceof Stub) : source + " cannot call a stub: " + t1xSafepoint.callee;
                    dcIndex++;
                }
                safepoints[safepointIndex] = t1xSafepoint;
            }
        }

        sig = initSig(method);

        objectLiterals = source.referenceLiterals();
        if (objectLiterals != null) {
            objectLiteralDataPatches = new int[objectLiterals.length];
            for (int i = 0; i < objectLiterals.length; i++) {
                int dispFromCodeStart = dispFromCodeStart(objectLiterals.length, 0, i, true);
                int patchPos = findDataPatchPos(source, dispFromCodeStart);
                FatalError.check(patchPos >= 0, source + ": could not find load of reference literal " + i + "\"" + objectLiterals[i] + "\"");
                objectLiteralDataPatches[i] = patchPos;
            }
        } else {
            objectLiteralDataPatches = null;
        }

    }

    @HOSTED_ONLY
    static String toHexString(byte[] bytes) {
        String result = "[";
        String separator = "";
        for (byte b : bytes) {
            result += separator + String.format("%02X", b);
            separator = " ";
        }
        result += "]";
        return result;
    }

    /**
     * Finds the platform dependent position needed to patch an instruction that loads
     * an object from the array immediately preceding the code array in memory.
     */
    @HOSTED_ONLY
    static int findDataPatchPos(MaxTargetMethod source, int dispFromCodeStart) {
        if (T1X.isAMD64()) {
            return AMD64T1XCompilation.findDataPatchPos(source, dispFromCodeStart);
        } else {
            throw T1X.unimplISA();
        }
    }

    @HOSTED_ONLY
    public Sig initSig(ClassMethodActor method) {
        CiRegisterConfig regConfig = vm().registerConfigs.standard;
        Map<Integer, Integer> slots = extractSlots(method);

        SignatureDescriptor sig = method.descriptor();
        Kind[] kinds = method.getParameterKinds();
        Arg[] in = new Arg[kinds.length];
        CiCallingConvention cc = regConfig.getCallingConvention(Type.RuntimeCall, WordUtil.ciKinds(kinds, true), target(), false);
        int localVarIndex = 0;
        for (int i = 0; i < kinds.length; i++) {
            Kind kind = kinds[i];
            Integer slotObj = slots.get(i);
            int slot = slotObj == null ? -1 : slotObj;
            assert cc.locations[i].isRegister() : "templates with non-reg args are not supported: " + method;
            CiRegister reg = cc.locations[i].asRegister();
            in[i] = new Arg(kind, reg, localVarName(method, localVarIndex, kind), slot);
            localVarIndex += kind.stackSlots;
        }
        Kind outKind = sig.resultKind();
        int outSlot = outKind == Kind.VOID ? -1 : 0;
        Slot slot = method.toJava().getAnnotation(Slot.class);
        if (slot != null) {
            outSlot = slot.value();
        }
        Arg out = new Arg(outKind, regConfig.getReturnRegister(WordUtil.ciKind(outKind, true)), null, outSlot);
        Sig s = new Sig(in, out);
        return s;
    }

    @HOSTED_ONLY
    private static Map<Integer, Integer> extractSlots(ClassMethodActor template) {
        SignatureDescriptor sig = template.descriptor();
        Annotation[][] annotations = template.toJava().getParameterAnnotations();
        Map<Integer, Integer> slots = new HashMap<Integer, Integer>(annotations.length);
        for (int i = 0; i < annotations.length; i++) {
            Slot s = null;
            for (Annotation a : annotations[i]) {
                if (a.annotationType() == Slot.class) {
                    s = (Slot) a;
                }
            }
            if (s != null) {
                assert !slots.containsValue(s.value()) : "operand stack index of " + sig.parameterDescriptorAt(i).toKind() + " parameter " + i + " of " + template + " conflicts with another parameter";
                slots.put(i, s.value());
            }
        }
        return slots;
    }

    @HOSTED_ONLY
    private static String localVarName(ClassMethodActor template, int localVarIndex, Kind kind) {
        CodeAttribute codeAttribute = template.codeAttribute();
        LocalVariableTable lvt = codeAttribute.localVariableTable();
        Entry e = lvt.findLocalVariable(localVarIndex, 0);
        assert e.descriptor(codeAttribute.cp).toKind() == kind;
        return e.name(codeAttribute.cp).string;
    }
}
