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
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

/**
 * Every object has a reference to its "hub" in its header.
 *
 * @author Bernd Mathiske
 */
public abstract class Hub extends Hybrid {

    /**
     * Indicates the "tuple cell size" for objects as follows.
     * - tuples:  the permanent cell size
     * - hybrids: the cell size before expansion
     * - arrays:  the object header size
     *
     * @return the "tuple size" of objects having this hub
     */
    public final Size tupleSize;
    public final Hub componentHub;
    public final SpecificLayout specificLayout;
    @INSPECTED
    public final ClassActor classActor;
    public final Layout.Category layoutCategory;
    public BiasedLockEpoch biasedLockEpoch = BiasedLockEpoch.init();
    @CONSTANT_WHEN_NOT_ZERO
    private static int firstWordIndex;
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
    public final boolean isSpecialReference;

    public WordWidth wordWidth() {
        return specificLayout.gripScheme().dataModel().wordWidth;
    }

    /**
     * @return whether the given serial numbers collide in a hash table of size 'divisor' using as "hash function" simply '% divisor'.
     */
    private boolean colliding(BitSet serials, int divisor) {
        final boolean[] hit = new boolean[divisor];
        for (int serial = serials.nextSetBit(0); serial >= 0; serial = serials.nextSetBit(serial + 1)) {
            final int index = serial % divisor;
            if (hit[index]) {
                return true;
            }
            hit[index] = true;
        }
        return false;
    }

    /**
     * @return the smallest table size for which we have perfect (collision free) hashing for the given classActors.
     */
    private int getMinCollisionFreeDivisor(BitSet serials) {
        int divisor = serials.cardinality();
        while (colliding(serials, divisor)) {
            divisor++;
        }
        return divisor;
    }

    private static int getFirstWordIndex() {
        if (firstWordIndex == 0) {
            final ClassActor classActor = ClassActor.fromJava(Hub.class);

            // Although the actual super class is 'Object', since it has no fields, we may pass 'null' here instead
            // and indeed we must to avoid not-yet-bootstrapped calls on the super class actor:
            final ClassActor superClassActor = null;

            final Size tupleSize = Layout.hybridLayout().layoutFields(superClassActor, classActor.localInstanceFieldActors());
            firstWordIndex = Layout.hybridLayout().firstAvailableWordArrayIndex(tupleSize);
        }
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
        return Unsigned.idiv((iTableStartIndex + iTableLength) * Word.size(), Ints.SIZE);
    }

    @Override
    public final int lastIntIndex() {
        return referenceMapStartIndex + referenceMapLength - 1;
    }

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

    private int getITableLength(BitSet superClassActorSerials, Iterable<InterfaceActor> allInterfaceActors) {
        int result = 1 + superClassActorSerials.cardinality();
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
    protected Hub(Size tupleSize, ClassActor classActor, TupleReferenceMap referenceMap) {
        this.tupleSize = tupleSize;
        this.componentHub = null;
        this.specificLayout = Layout.tupleLayout();
        this.layoutCategory = Layout.Category.TUPLE;
        this.classActor = classActor;
        this.iTableStartIndex = firstWordIndex(); // the vTable is unused in static hubs
        this.iTableLength = 1;
        this.mTableStartIndex = firstIntIndex();
        this.mTableLength = 1;
        this.referenceMapStartIndex = mTableStartIndex + mTableLength;
        this.referenceMapLength = referenceMap.numberOfEntries();
        this.isSpecialReference = false;
    }

    /**
     * Dynamic Hub.
     */
    protected Hub(Size tupleSize, SpecificLayout specificLayout, ClassActor classActor, BitSet superClassActorSerials, Iterable<InterfaceActor> allInterfaceActors, int vTableLength, TupleReferenceMap referenceMap) {
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
        this.iTableLength = getITableLength(superClassActorSerials, allInterfaceActors);
        this.mTableStartIndex = firstIntIndex();
        this.mTableLength = getMinCollisionFreeDivisor(superClassActorSerials);
        this.referenceMapStartIndex = mTableStartIndex + mTableLength;
        this.referenceMapLength = referenceMap.numberOfEntries();
        this.isSpecialReference = classActor.isSpecialReference();
    }

    protected final Hub expand() {
        return (Hub) expand(Unsigned.idiv(Ints.roundUnsignedUpByPowerOfTwo((referenceMapStartIndex + referenceMapLength) * Ints.SIZE, Word.size()), Word.size()));
    }

    @INLINE
    public final int getMTableIndex(int serial) {
        return (serial % mTableLength) + mTableStartIndex;
    }

    @INLINE
    public final int getITableIndex(int serial) {
        return getInt(getMTableIndex(serial));
    }

    @INLINE
    public final boolean isSubClassHub(ClassActor testClassActor) {
        if (this.classActor == testClassActor) {
            // the common case of an exact type match
            return true;
        }
        final int serial = testClassActor.id;
        final int iTableIndex = getITableIndex(serial);
        return getWord(iTableIndex).equals(Address.fromInt(serial));
    }

    public abstract FieldActor findFieldActor(int offset);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + classActor + "]";
    }
}
