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
package com.sun.max.vm.compiler.cps.cir.gui;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.cir.*;

/**
 * A textual trace of a CIR graph annotated by {@linkplain Element descriptions of the position(s) of each CIR node}
 * within the trace.
 *
 * @author Doug Simon
 * @author Sumeet Panchal
 */
class CirAnnotatedTrace implements Iterable<CirAnnotatedTrace.Element> {

    private final String trace;

    private final Sequence<Element> elements;

    private final ClassMethodActor classMethodActor;

    private final String description;

    /**
     * Describes the {@linkplain Range range(s)} in the trace of a single CIR node.
     */
    public abstract static class Element {

        private final CirNode node;

        protected Element(CirNode node) {
            this.node = node;
        }

        public CirNode node() {
            return node;
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
            if (node == null) {
                return "";
            }
            return node.getClass().getSimpleName();
        }

        @Override
        public abstract String toString();
    }

    /**
     * A {@code SimpleElement} associates a CIR node with a single {@linkplain #visitRanges(RangeVisitor) primary range} in a trace.
     */
    public static class SimpleElement extends CirAnnotatedTrace.Element {

        private Range range;

        public SimpleElement(CirNode node, Range range) {
            super(node);
            this.range = range;
        }

        public Range range() {
            return range;
        }

        @Override
        public void visitRanges(RangeVisitor visitor) {
            visitor.visitRange(range);
        }

        @Override
        public String toString() {
            return nodeType() + range;
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

        private Range open;
        private Range close;

        public ParenthesisElement(Range open, Range close) {
            super(null);
            this.open = open;
            this.close = close;
        }

        @Override
        public void visitRanges(RangeVisitor visitor) {
            visitor.visitRange(open);
            visitor.visitRange(close);
        }

        @Override
        public String toString() {
            return nodeType() + '{' + open + ',' + close + '}';
        }

        public Range firstRange() {
            return open;
        }

        public Range secondRange() {
            return close;
        }
    }

    /**
     * A {@code MultiRangeElement} associates a CIR node with more than 2 {@linkplain #visitRanges(RangeVisitor) primary ranges} in a trace.
     */
    public static class MultiRangeElement extends CirAnnotatedTrace.Element {

        private final Sequence<Range> ranges;

        public MultiRangeElement(CirNode node, Sequence<Range> ranges) {
            super(node);
            assert !ranges.isEmpty();
            this.ranges = ranges;
        }

        @Override
        public void visitRanges(RangeVisitor visitor) {
            for (Range range : ranges) {
                visitor.visitRange(range);
            }
        }

        @Override
        public String toString() {
            return nodeType() + '{' + Sequence.Static.toString(ranges, null, ",") + '}';
        }
    }

    public CirAnnotatedTrace(String trace, Sequence<Element> elements, ClassMethodActor classMethodActor, String description) {
        this.trace = trace;
        this.elements = elements;
        this.classMethodActor = classMethodActor;
        this.description = description == null ? "" : description;
    }

    public String trace() {
        return trace;
    }

    public Sequence<Element> elements() {
        return elements;
    }

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    public String description() {
        return description;
    }

    public Iterator<Element> iterator() {
        return elements.iterator();
    }
}
