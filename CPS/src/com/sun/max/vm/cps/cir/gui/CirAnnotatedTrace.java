/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.gui;

import java.util.*;

import com.sun.max.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.cir.*;

/**
 * A textual trace of a CIR graph annotated by {@linkplain Element descriptions of the position(s) of each CIR node}
 * within the trace.
 *
 * @author Doug Simon
 * @author Sumeet Panchal
 */
class CirAnnotatedTrace implements Iterable<CirAnnotatedTrace.Element> {

    private final String trace;

    private final List<Element> elements;

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

        private final List<Range> ranges;

        public MultiRangeElement(CirNode node, List<Range> ranges) {
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
            return nodeType() + '{' + ranges + '}';
        }
    }

    public CirAnnotatedTrace(String trace, List<Element> elements, ClassMethodActor classMethodActor, String description) {
        this.trace = trace;
        this.elements = elements;
        this.classMethodActor = classMethodActor;
        this.description = description == null ? "" : description;
    }

    public String trace() {
        return trace;
    }

    public List<Element> elements() {
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
