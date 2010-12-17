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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.Category;
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
    public BiasedLockEpoch64 biasedLockEpoch = BiasedLockEpoch64.init();
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
    public final boolean isSpecialReference;

    /**
     * Determines whether a given set of class ids collide in a hash table of size {@code divisor}
     * using a hash function of {@code id % divisor}.
     *
     * @param set of class ids with interface ids encoded as their negative value
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
     * @param set of class ids with interface ids encoded as their negative value
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

    private static int getFirstWordIndex() {
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
    protected Hub(Size tupleSize, SpecificLayout specificLayout, ClassActor classActor, int[] superClassActorIds, Iterable<InterfaceActor> allInterfaceActors, int vTableLength, TupleReferenceMap referenceMap) {
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
        this.isSpecialReference = classActor.isSpecialReference();
    }

    protected final Hub expand() {
        return (Hub) expand(computeLength(referenceMapStartIndex, referenceMapLength));
    }

    /**
     * Computes the number of words that the non-header part of a hub occupies. That is,
     * if a hub is viewed as a word array, the returned value is the length of the array.
     */
    protected static int computeLength(int referenceMapStartIndex, int referenceMapLength) {
        int referenceMapSize = Ints.roundUnsignedUpByPowerOfTwo((referenceMapStartIndex + referenceMapLength) * Ints.SIZE, Word.size());
        return Unsigned.idiv(referenceMapSize, Word.size());
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
}
