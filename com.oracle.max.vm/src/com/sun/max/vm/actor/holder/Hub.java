/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.holder;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.type.ClassRegistry.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.Category;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Every object has a reference to its "hub" in its header.
 */
public abstract class Hub extends Hybrid {

    /**
     * Indicates the "tuple cell size" for objects as follows.
     * - tuples:  the permanent cell size
     * - hybrids: the cell size before expansion
     * - arrays:  the object header size
     */
    public final Size tupleSize;
    public final Hub componentHub;
    public final SpecificLayout specificLayout;
    @INSPECTED
    public final ClassActor classActor;
    public final Layout.Category layoutCategory;
    public BiasedLockEpoch64 biasedLockEpoch = BiasedLockEpoch64.init();
    /**
     * Word index to the first element of the variable-size part of the hub.
     * The index is relative to the first word following the hub's header.
     * Note that all indexes to virtual and interface tables are also relative to the first word following the hub's header.
     */
    private static final int firstWordIndex;
    public final int iTableStartIndex;
    public final int iTableLength;
    @INSPECTED
    public final int mTableStartIndex;
    @INSPECTED
    public final int mTableLength;
    @INSPECTED
    public final int referenceMapLength;
    @INSPECTED
    public final int referenceMapStartIndex;

    /**
     * Specifies if this is the hub for {@link java.lang.ref.Reference} or
     * a subclass of the former.
     */
    public final boolean isJLRReference;

    /**
     * Determines whether a given set of class ids collide in a hash table of size {@code divisor}
     * using a hash function of {@code id % divisor}.
     *
     * @param ids set of class ids with interface ids encoded as their negative value
     * @see ClassID
     */
    private static boolean colliding(int[] ids, int divisor) {
        if (divisor < 64) {
            // Common case avoids allocating boolean[]
            long table = 0;
            for (int id : ids) {
                int posId = id >= 0 ? id : -id;
                final int index = posId % divisor;
                long entry = 1L << index;
                if ((table & entry) != 0) {
                    return true;
                }
                table |= entry;
            }
            return false;
        }
        final boolean[] table = new boolean[divisor];
        for (int id : ids) {
            int posId = id >= 0 ? id : -id;
            final int index = posId % divisor;
            if (table[index]) {
                return true;
            }
            table[index] = true;
        }
        return false;
    }

    /**
     * Gets the smallest table size for which we have perfect (collision free) hashing for the given class ids.
     *
     * @param ids set of class ids with interface ids encoded as their negative value
     */
    private static int minCollisionFreeDivisor(int[] ids) {
        int divisor = ids.length;
        while (colliding(ids, divisor)) {
            divisor++;
        }
        return divisor;
    }

    static {
        final ClassActor classActor = ClassActor.fromJava(Hub.class);

        // Although the actual super class is 'Object', since it has no fields, we may pass 'null' here instead
        // and indeed we must to avoid not-yet-bootstrapped calls on the super class actor:
        final ClassActor superClassActor = null;

        final Size tupleSize = Layout.hybridLayout().layoutFields(superClassActor, classActor.localInstanceFieldActors());
        firstWordIndex = Layout.hybridLayout().firstAvailableWordArrayIndex(tupleSize);
    }

    private static int computeFirstWordIndex() {
        final ClassActor classActor = ClassActor.fromJava(Hub.class);

        // Although the actual super class is 'Object', since it has no fields, we may pass 'null' here instead
        // and indeed we must to avoid not-yet-bootstrapped calls on the super class actor:
        final ClassActor superClassActor = null;

        final Size tupleSize = Layout.hybridLayout().layoutFields(superClassActor, classActor.localInstanceFieldActors());
        return Layout.hybridLayout().firstAvailableWordArrayIndex(tupleSize);
    }

    @FOLD
    public static int getFirstWordIndex() {
        return firstWordIndex;
    }

    @Override
    public final int firstWordIndex() {
        return getFirstWordIndex();
    }

    @Override
    public final int lastWordIndex() {
        return iTableStartIndex + iTableLength - 1;
    }

    @Override
    public final int firstIntIndex() {
        return UnsignedMath.divide((iTableStartIndex + iTableLength) * Word.size(), Ints.SIZE);
    }

    @Override
    public final int lastIntIndex() {
        return referenceMapStartIndex + referenceMapLength - 1;
    }

    /**
     * Index, relative to the first word following the hub's header, to the first entry of the virtual table.
     * @return an word index
     */
    @INLINE
    public static int vTableStartIndex() {
        return getFirstWordIndex();
    }

    public final int vTableLength() {
        return iTableStartIndex - vTableStartIndex();
    }

    @CONSTANT_WHEN_NOT_ZERO
    private BiasedLockRevocationHeuristics biasedLockRevocationHeuristics;

    @INLINE
    public final BiasedLockRevocationHeuristics biasedLockRevocationHeuristics() {
        return biasedLockRevocationHeuristics;
    }

    public void setBiasedLockRevocationHeuristics(BiasedLockRevocationHeuristics biasedLockRevocationHeuristics) {
        this.biasedLockRevocationHeuristics = biasedLockRevocationHeuristics;
    }

    private int getITableLength(int[] superClassActorIds, Iterable<InterfaceActor> allInterfaceActors) {
        int result = 1 + superClassActorIds.length;
        if (classActor.isReferenceClassActor()) {
            for (InterfaceActor interfaceActor : allInterfaceActors) {
                result += interfaceActor.localInterfaceMethodActors().length;
            }
        }
        return result;
    }

    /**
     * Static Hub.
     */
    protected Hub(Size tupleSize, ClassActor classActor, TupleReferenceMap referenceMap, int vTableLength) {
        this.tupleSize = tupleSize;
        this.componentHub = null;
        this.specificLayout = Layout.tupleLayout();
        this.layoutCategory = Layout.Category.TUPLE;
        this.classActor = classActor;
        this.iTableStartIndex = firstWordIndex() + vTableLength;
        this.iTableLength = 1;
        this.mTableStartIndex = firstIntIndex();
        this.mTableLength = 1;
        this.referenceMapStartIndex = mTableStartIndex + mTableLength;
        this.referenceMapLength = referenceMap.numberOfEntries();
        this.isJLRReference = false;
    }

    /**
     * Dynamic Hub.
     */
    protected Hub(Size tupleSize,
                  SpecificLayout specificLayout,
                  ClassActor classActor,
                  int[] superClassActorIds,
                  Iterable<InterfaceActor> allInterfaceActors,
                  int vTableLength,
                  TupleReferenceMap referenceMap) {
        this.tupleSize = tupleSize;
        this.specificLayout = specificLayout;
        this.layoutCategory = specificLayout.category();

        if (layoutCategory == Category.ARRAY) {
            componentHub = classActor.componentClassActor().dynamicHub();
            assert componentHub != null || classActor.componentClassActor().kind != Kind.REFERENCE;
        } else {
            componentHub = null;
        }

        this.classActor = classActor;
        this.iTableStartIndex = firstWordIndex() + vTableLength;
        this.iTableLength = getITableLength(superClassActorIds, allInterfaceActors);
        this.mTableStartIndex = firstIntIndex();
        this.mTableLength = minCollisionFreeDivisor(superClassActorIds);
        this.referenceMapStartIndex = mTableStartIndex + mTableLength;
        this.referenceMapLength = referenceMap.numberOfEntries();
        this.isJLRReference = isSupertypeOf(JLR_REFERENCE, classActor);
    }

    private static boolean isSupertypeOf(ClassActor c, ClassActor sub) {
        while (sub != null) {
            if (sub == c) {
                return true;
            }
            sub = sub.superClassActor;
        }
        return false;
    }

    protected final Hub expand() {
        return (Hub) expand(computeLength(referenceMapStartIndex, referenceMapLength));
    }

    static Address checkCompiled(VirtualMethodActor virtualMethodActor) {
        if (!MaxineVM.isHosted()) {
            final TargetMethod current = virtualMethodActor.currentTargetMethod();
            if (current != null) {
                return current.getEntryPoint(CallEntryPoint.VTABLE_ENTRY_POINT).toAddress();
            }
        }
        return Address.zero();
    }

    void initializeVTable(VirtualMethodActor[] allVirtualMethodActors) {
        for (int i = 0; i < allVirtualMethodActors.length; i++) {
            final VirtualMethodActor virtualMethodActor = allVirtualMethodActors[i];
            final int vTableIndex = vTableStartIndex() + i;
            assert virtualMethodActor.vTableIndex() == vTableIndex;
            assert getWord(vTableIndex).isZero();
            Address vTableEntry;
            if (MaxineVM.isHosted()) {
                vTableEntry = checkCompiled(virtualMethodActor);
                if (vTableEntry.isZero()) {
                    vTableEntry = vm().stubs.virtualTrampoline(vTableIndex).toAddress();
                }
            } else {
                // IMPORTANT: Don't fill with compiled method entry points.
                // This is delayed to until the class actor for this class is actually entered in the class hierarchy.
                // There's a couple of reasons for that:
                // 1. A deoptimization may occur between the initialization of the vtable and the insertion of the class actor to the class hierarchy graph.
                // In this interval of time, the class actor is not visible to the deoptimization process. By the time it becomes visible, the deopt is complete
                // and the vtable is corrupted with code pointers to obsolete optimized method whose entry point has been patched a jump to static trampoline.
                // This will cause subsequent vtable dispatch to fail as these will take the trampoline, which will attempt to patch what it believe was a direct call.
                // This results in an fatal error because the call site isn't a direct call but a register indirect call of a vtable dispatch.
                //
                // 2. multiple threads may race to create a ClassActor for the same class as they race for their definition. There's no need to waste time
                // checking for compiled code until the class actor is effectively selected to define the class (which is effected when adding to the class hierarchy.
                // This will be done with the method optimized vtable below.
                vTableEntry = vm().stubs.virtualTrampoline(vTableIndex).toAddress();
            }
            setWord(vTableIndex, vTableEntry);
        }
    }

    private void checkVTableEntry(int vTableIndex) {
        final Address vTableEntry = getWord(vTableIndex).asAddress();
        TargetMethod tm = Code.codePointerToTargetMethod(vTableEntry.asPointer());
        if (tm == null || Stubs.isJumpToStaticTrampoline(tm)) {
            Log.println(classActor.toString() + "(hub = " + Reference.fromJava(this).toOrigin().to0xHexString() +
                            ") has virtual table entry #" + (vTableIndex - firstWordIndex) + "(" + vTableIndex + ") points to method patched with static trampoline");
            FatalError.unexpected("corrupted vtable");
        }
    }

    public void checkVTable() {
        if (!MaxineVM.isHosted()) {
            final VirtualMethodActor[] allVirtualMethodActors = classActor.allVirtualMethodActors();
            final int endOfVTable = firstWordIndex() + allVirtualMethodActors.length;
            for (int vTableIndex = firstWordIndex(); vTableIndex < endOfVTable; vTableIndex++) {
                checkVTableEntry(vTableIndex);
            }
        }
    }

    /**
     * Replace vtable trampoline with compiled method entry points for compiled virtual methods.
     * This is done once, upon adding the class actor to the class hierarchy.
     * @see DependenciesManager
     */
    public void refreshVTable() {
        final VirtualMethodActor[] allVirtualMethodActors = classActor.allVirtualMethodActors();
        for (int i = 0; i < allVirtualMethodActors.length; i++) {
            final VirtualMethodActor virtualMethodActor = allVirtualMethodActors[i];
            final Address vTableEntry = checkCompiled(virtualMethodActor);
            if (vTableEntry.isNotZero()) {
                setWord(vTableStartIndex() + i, vTableEntry);
            }
            if (MaxineVM.isDebug()) {
                checkVTableEntry(vTableStartIndex() + i);
            }
        }
    }

    /**
     * Make a given vtable entry points to the trampoline again.
     * @param index the offset into the vtable as answered by {@linkplain VirtualMethodActor#vTableIndex()}
     */
    public void resetVTableEntry(int index) {
        setWord(index, vm().stubs.virtualTrampoline(index).toAddress());
    }

    /**
     * Computes the number of words that the non-header part of a hub occupies. That is,
     * if a hub is viewed as a word array, the returned value is the length of the array.
     */
    protected static int computeLength(int referenceMapStartIndex, int referenceMapLength) {
        int referenceMapSize = Ints.roundUnsignedUpByPowerOfTwo((referenceMapStartIndex + referenceMapLength) * Ints.SIZE, Word.size());
        return UnsignedMath.divide(referenceMapSize, Word.size());
    }

    @INLINE
    public final int getMTableIndex(int id) {
        return (id % mTableLength) + mTableStartIndex;
    }

    @INLINE
    public final int getITableIndex(int id) {
        return getInt(getMTableIndex(id));
    }

    @INLINE
    public final boolean isSubClassHub(ClassActor testClassActor) {
        if (this.classActor == testClassActor) {
            // the common case of an exact type match
            return true;
        }
        final int id = testClassActor.id;
        final int iTableIndex = getITableIndex(id);
        return getWord(iTableIndex).equals(Address.fromInt(id));
    }

    public abstract FieldActor findFieldActor(int offset);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + classActor + "]";
    }

    public static boolean validItableEntry(CodePointer p) {
        final long pvalue = p.toLong();
        if (pvalue < 0 || pvalue > Integer.MAX_VALUE) {
            return false;
        }
        final int id = (int) pvalue;
        return ClassID.toClassActor(id) != null || ClassID.isUsedID(id);
    }

    /**
     * Visit references of the object described by this hub  using the hub's reference maps.
     * Note that reference arrays have an empty reference map.
     */
    @INLINE
    public final void visitMappedReferences(Pointer origin, PointerIndexVisitor visitor) {
        final int n = referenceMapStartIndex + referenceMapLength;
        for (int i = referenceMapStartIndex; i < n; i++) {
            final int index = getInt(i);
            visitor.visit(origin, index);
        }
    }
}
