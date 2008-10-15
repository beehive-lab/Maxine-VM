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
package com.sun.max.vm.compiler.cir.gui;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.*;

/**
 * A textual trace of a CIR graph annotated by {@linkplain Element descriptions of the position(s) of each CIR node}
 * within the trace.
 * 
 * @author Doug Simon
 * @author Sumeet Panchal
 */
class CirAnnotatedTrace implements Iterable<CirAnnotatedTrace.Element> {

    private final String _trace;

    private final Sequence<Element> _elements;

    private final ClassMethodActor _classMethodActor;

    private final String _description;

    /**
     * Describes the {@linkplain Range range(s)} in the trace of a single CIR node.
     */
    public abstract static class Element {

        private final CirNode _node;

        protected Element(CirNode node) {
            _node = node;
        }

        public CirNode node() {
            return _node;
        }

        /**
         * Iterates over the range(s) in a trace denoting the primary text for a CIR node.
         * For example, the primary text for a {@link CirClosure} is "proc".
         */
        public abstract void visitRanges(RangeVisitor visitor);

        /**
         * Iterates over the range(s) of secondary text associated with a CIR node.
         * For example, the secondary text for a {@link CirClosure} is the text for its
         * {@linkplain CirClosure#parameters() parameters} and {@linkplain CirClosure#body() body}.
         */
        public void visitAssociatedRanges(RangeVisitor visitor) {
        }

        /**
         * Gets the {@linkplain Class#getSimpleName() simple name} of the type of this element's
         * {@linkplain #node() node}.
         * 
         * @return {@code ""} if {@code node() == null}
         */
        public final String nodeType() {
            if (_node == null) {
                return "";
            }
            return _node.getClass().getSimpleName();
        }

        @Override
        public abstract String toString();
    }

    /**
     * A {@code SimpleElement} associates a CIR node with a single {@linkplain #visitRanges(RangeVisitor) primary range} in a trace.
     */
    public static class SimpleElement extends CirAnnotatedTrace.Element {

        private Range _range;

        public SimpleElement(CirNode node, Range range) {
            super(node);
            _range = range;
        }

        public Range range() {
            return _range;
        }

        @Override
        public void visitRanges(RangeVisitor visitor) {
            visitor.visitRange(_range);
        }

        @Override
        public String toString() {
            return nodeType() + _range;
        }

        @Override
        public boolean equals(Object other) {
            return node() == ((CirAnnotatedTrace.Element) other).node();
        }

        @Override
        public int hashCode() {
            return node().hashCode();
        }

    }

    /**
     * A {@code DualRangeElement} associates a CIR node with 2 {@linkplain #visitRanges(RangeVisitor) primary ranges} in a trace.
     */
    public static class ParenthesisElement extends CirAnnotatedTrace.Element {

        private Range _open;
        private Range _close;

        public ParenthesisElement(Range open, Range close) {
            super(null);
            _open = open;
            _close = close;
        }

        @Override
        public void visitRanges(RangeVisitor visitor) {
            visitor.visitRange(_open);
            visitor.visitRange(_close);
        }

        @Override
        public String toString() {
            return nodeType() + '{' + _open + ',' + _close + '}';
        }

        public Range firstRange() {
            return _open;
        }

        public Range secondRange() {
            return _close;
        }
    }

    /**
     * A {@code MultiRangeElement} associates a CIR node with more than 2 {@linkplain #visitRanges(RangeVisitor) primary ranges} in a trace.
     */
    public static class MultiRangeElement extends CirAnnotatedTrace.Element {

        private final Sequence<Range> _ranges;

        public MultiRangeElement(CirNode node, Sequence<Range> ranges) {
            super(node);
            assert !ranges.isEmpty();
            _ranges = ranges;
        }

        @Override
        public void visitRanges(RangeVisitor visitor) {
            for (Range range : _ranges) {
                visitor.visitRange(range);
            }
        }

        @Override
        public String toString() {
            return nodeType() + '{' + Sequence.Static.toString(_ranges, null, ",") + '}';
        }
    }

    public CirAnnotatedTrace(String trace, Sequence<Element> elements, ClassMethodActor classMethodActor, String description) {
        _trace = trace;
        _elements = elements;
        _classMethodActor = classMethodActor;
        _description = description == null ? "" : description;
    }

    public String trace() {
        return _trace;
    }

    public Sequence<Element> elements() {
        return _elements;
    }

    public ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    public String description() {
        return _description;
    }

    public Iterator<Element> iterator() {
        return _elements.iterator();
    }
}
