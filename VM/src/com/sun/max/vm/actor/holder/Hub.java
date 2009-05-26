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
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

/**
 * Every object has a reference to its "hub" in its header.
 *
 * @author Bernd Mathiske
 */
public abstract class Hub extends Hybrid {

    private final Size _tupleSize;
    private final Kind _elementKind;
    private final SpecificLayout _specificLayout;
    @INSPECTED
    private final ClassActor _classActor;
    private final Layout.Category _layoutCategory;
    private BiasedLockEpoch _biasedLockEpoch = BiasedLockEpoch.init();
    @CONSTANT_WHEN_NOT_ZERO
    private static int _firstWordIndex;
    private final int _iTableStartIndex;
    private final int _iTableLength;
    @INSPECTED
    private final int _mTableStartIndex;
    @INSPECTED
    private final int _mTableLength;
    @INSPECTED
    private final int _referenceMapLength;
    private final boolean _isSpecialReference;

    /**
     * Indicate the "tuple cell size" for objects as follows.
     * - tuples:  the permanent cell size
     * - hybrids: the cell size before expansion
     * - arrays:  the object header size
     *
     * @return the "tuple size" of objects having this hub
     */
    @INLINE
    public final Size tupleSize() {
        return _tupleSize;
    }

    @INLINE
    public final Kind elementKind() {
        return _elementKind;
    }

    @INLINE
    public final SpecificLayout specificLayout() {
        return _specificLayout;
    }

    public WordWidth wordWidth() {
        return _specificLayout.gripScheme().dataModel().wordWidth();
    }

    @INLINE
    public final ClassActor classActor() {
        return _classActor;
    }

    @INLINE
    public final Layout.Category layoutCategory() {
        return _layoutCategory;
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
        if (_firstWordIndex == 0) {
            final ClassActor classActor = ClassActor.fromJava(Hub.class);

            // Although the actual super class is 'Object', since it has no fields, we may pass 'null' here instead
            // and indeed we must to avoid not-yet-bootstrapped calls on the super class actor:
            final ClassActor superClassActor = null;

            final Size tupleSize = Layout.hybridLayout().layoutFields(superClassActor, classActor.localInstanceFieldActors());
            _firstWordIndex = Layout.hybridLayout().firstAvailableWordArrayIndex(tupleSize);
        }
        return _firstWordIndex;
    }

    @Override
    public final int firstWordIndex() {
        return getFirstWordIndex();
    }

    @Override
    public final int lastWordIndex() {
        return _iTableStartIndex + _iTableLength - 1;
    }

    @Override
    public final int firstIntIndex() {
        return Unsigned.idiv((_iTableStartIndex + _iTableLength) * Word.size(), Ints.SIZE);
    }

    @Override
    public final int lastIntIndex() {
        return _referenceMapStartIndex + _referenceMapLength - 1;
    }

    @INLINE
    public static int vTableStartIndex() {
        return getFirstWordIndex();
    }

    public final int vTableLength() {
        return _iTableStartIndex - vTableStartIndex();
    }

    public final int iTableStartIndex() {
        return _iTableStartIndex;
    }

    public final int iTableLength() {
        return _iTableLength;
    }

    @INLINE
    public final int mTableStartIndex() {
        return _mTableStartIndex;
    }

    @INLINE
    public final int mTableLength() {
        return _mTableLength;
    }

    @INSPECTED
    private final int _referenceMapStartIndex;

    @INLINE
    public final int referenceMapStartIndex() {
        return _referenceMapStartIndex;
    }

    @INLINE
    public final int referenceMapLength() {
        return _referenceMapLength;
    }

    @INLINE
    public final BiasedLockEpoch biasedLockEpoch() {
        return _biasedLockEpoch;
    }

    @INLINE
    public final boolean isSpecialReference() {
        return _isSpecialReference;
    }

    public void setBiasedLockEpoch(BiasedLockEpoch biasedLockEpoch) {
        _biasedLockEpoch = biasedLockEpoch;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private BiasedLockRevocationHeuristics _biasedLockRevocationHeuristics;

    @INLINE
    public final BiasedLockRevocationHeuristics biasedLockRevocationHeuristics() {
        return _biasedLockRevocationHeuristics;
    }

    public void setBiasedLockRevocationHeuristics(BiasedLockRevocationHeuristics biasedLockRevocationHeuristics) {
        _biasedLockRevocationHeuristics = biasedLockRevocationHeuristics;
    }

    private int getITableLength(BitSet superClassActorSerials, Iterable<InterfaceActor> allInterfaceActors) {
        int result = 1 + superClassActorSerials.cardinality();
        if (_classActor.isReferenceClassActor()) {
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
        _tupleSize = tupleSize;
        _elementKind = Kind.VOID;
        _specificLayout = Layout.tupleLayout();
        _layoutCategory = Layout.Category.TUPLE;
        _classActor = classActor;
        _iTableStartIndex = firstWordIndex(); // the vTable is unused in static hubs
        _iTableLength = 1;
        _mTableStartIndex = firstIntIndex();
        _mTableLength = 1;
        _referenceMapStartIndex = _mTableStartIndex + _mTableLength;
        _referenceMapLength = referenceMap.numberOfOffsets();
        _isSpecialReference = false;
    }

    /**
     * Dynamic Hub.
     */
    protected Hub(Size tupleSize, SpecificLayout specificLayout, ClassActor classActor, BitSet superClassActorSerials, Iterable<InterfaceActor> allInterfaceActors, int vTableLength, TupleReferenceMap referenceMap) {
        _tupleSize = tupleSize;
        _specificLayout = specificLayout;
        _layoutCategory = specificLayout.category();
        switch (_layoutCategory) {
            case ARRAY:
                _elementKind = classActor.componentClassActor().kind();
                break;
            case HYBRID:
                _elementKind = Kind.WORD;
                break;
            default:
                _elementKind = Kind.VOID;
                break;
        }
        _classActor = classActor;
        _iTableStartIndex = firstWordIndex() + vTableLength;
        _iTableLength = getITableLength(superClassActorSerials, allInterfaceActors);
        _mTableStartIndex = firstIntIndex();
        _mTableLength = getMinCollisionFreeDivisor(superClassActorSerials);
        _referenceMapStartIndex = _mTableStartIndex + _mTableLength;
        _referenceMapLength = referenceMap.numberOfOffsets();
        _isSpecialReference = classActor.isSpecialReference();
    }

    protected final Hub expand() {
        return (Hub) expand(Unsigned.idiv(Ints.roundUp((_referenceMapStartIndex + _referenceMapLength) * Ints.SIZE, Word.size()), Word.size()));
    }

    @INLINE
    public final int getMTableIndex(int serial) {
        return (serial % mTableLength()) + mTableStartIndex();
    }

    @INLINE
    public final int getITableIndex(int serial) {
        return getInt(getMTableIndex(serial));
    }

    @INLINE
    public final boolean isSubClassHub(ClassActor classActor) {
        if (classActor == _classActor) {
            // the common case of an exact type match
            return true;
        }
        final int serial = classActor.id();
        final int iTableIndex = getITableIndex(serial);
        return getWord(iTableIndex).equals(Address.fromInt(serial));
    }

    public abstract FieldActor findFieldActor(int offset);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + classActor() + "]";
    }
}
