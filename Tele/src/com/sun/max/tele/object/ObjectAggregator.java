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
/*VCSID=eb844bb9-9043-4e89-a29c-0463b23437a0*/
package com.sun.max.tele.object;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * An object for aggregating the objects in the heap based on type.
 */
public abstract class ObjectAggregator {

    private static int _alignment;

    public static ObjectAggregator create(ReferenceClassActor actor, Pointer base) {
        if (actor.toJava() == StaticTuple.class) {
            return new StaticTupleAggregator(base);
        } else if (actor.isArrayClassActor()) {
            return new ArrayAggregator((ArrayClassActor) actor, base);
        } else if (actor.isTupleClassActor()) {
            return new TupleAggregator((TupleClassActor) actor, base);
        } else {
            return new HybridAggregator((HybridClassActor) actor, base);
        }
    }

    final ReferenceClassActor _actor;
    private final BitSet _instances;
    private final Pointer _base;
    private int _count = -1;
    private Size _totalSize = Size.zero();

    ObjectAggregator(ReferenceClassActor actor, Pointer base) {
        _actor = actor;
        _instances = new BitSet();
        _base = base;
    }

    public final int count() {
        if (_count == -1) {
            _count = _instances.cardinality();
        }
        return _count;
    }

    public abstract boolean isArray();

    public ReferenceClassActor type() {
        return _actor;
    }

    public Iterator<Reference> instances(final TeleVM teleVM) {
        return new Iterator<Reference>() {
            int _nextWordOffset = _instances.nextSetBit(0);
            public boolean hasNext() {
                return _nextWordOffset >= 0;
            }

            public Reference next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final int wordOffset = _nextWordOffset;
                final int offset = wordOffset * _alignment;
                final Reference reference = teleVM.wordToReference(_base.plus(offset));
                _nextWordOffset = _instances.nextSetBit(wordOffset + 1);
                return reference;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Adds an object to the tally.
     *
     * @param reference the object to count
     * @return the size of the cell allocated for the object
     */
    public final Size add(TeleVM teleVM, int offset) {
        if (_alignment == 0) {
            _alignment = teleVM.vmConfiguration().platform().processorKind().dataModel().alignment().numberOfBytes();
        }

        final Pointer cell = _base.plus(offset);
        final Size size = sizeOf(teleVM, cell);
        _totalSize = _totalSize.plus(size);
        final int wordOffset = offset / _alignment;
        _instances.set(wordOffset);
        _count = -1;
        return size;
    }

    protected abstract Size sizeOf(TeleVM teleVM, Pointer cell);

    public final Size size() {
        return _totalSize;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ObjectAggregator && _actor.equals(((ObjectAggregator) other)._actor);
    }

    @Override
    public int hashCode() {
        return _actor.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + type() + " count=" + count() + " size=" + size() + "]";
    }

    /**
     * Aggregator for static tuples.
     */
    static class StaticTupleAggregator extends ObjectAggregator {

        StaticTupleAggregator(Pointer base) {
            super((ReferenceClassActor) ClassActor.fromJava(StaticTuple.class), base);
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        protected Size sizeOf(TeleVM teleVM, Pointer cell) {
            final Reference reference = teleVM.cellToReference(cell);
            final Hub hub = teleVM.makeLocalHubForObject(reference);
            return hub.tupleSize();
        }
    }

    /**
     * Aggregator for tuple objects.
     */
    static class TupleAggregator extends ObjectAggregator {

        private final Size _tupleSize;

        TupleAggregator(TupleClassActor classActor, Pointer base) {
            super(classActor, base);
            _tupleSize = classActor.dynamicTupleSize();
        }

        @Override
        protected Size sizeOf(TeleVM teleVM, Pointer cell) {
            return _tupleSize;
        }

        @Override
        public boolean isArray() {
            return false;
        }
    }

    /**
     * Aggregator for hubs.
     */
    static class HybridAggregator extends ObjectAggregator {

        private final HybridLayout _layout;

        HybridAggregator(HybridClassActor hybridActor, Pointer base) {
            super(hybridActor, base);
            _layout = Layout.hybridLayout();
        }

        @Override
        protected Size sizeOf(TeleVM teleVM, Pointer cell) {
            final Reference reference = teleVM.cellToReference(cell);
            final int length = _layout.readLength(reference);
            return _layout.getArraySize(length);
        }

        @Override
        public boolean isArray() {
            return false;
        }
    }

    /**
     * Aggregator for array objects.
     */
    static class ArrayAggregator extends ObjectAggregator {

        private final ArrayLayout _layout;

        ArrayAggregator(ArrayClassActor arrayClassActor, Pointer base) {
            super(arrayClassActor, base);
            _layout = arrayClassActor.componentClassActor().arrayLayout();
        }

        @Override
        protected Size sizeOf(TeleVM teleVM, Pointer cell) {
            final Reference reference = teleVM.cellToReference(cell);
            final int length = _layout.readLength(reference);
            final Size size = _layout.getArraySize(length);
            return size;
        }

        @Override
        public boolean isArray() {
            return true;
        }
    }
}
