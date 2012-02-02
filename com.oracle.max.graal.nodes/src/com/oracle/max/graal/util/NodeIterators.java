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
package com.oracle.max.graal.util;

import java.util.*;

import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;

public class NodeIterators {

    public abstract static class NodePredicate {
        public abstract boolean apply(Node n);

        public NodePredicate and(final NodePredicate np) {
            final NodePredicate thiz = this;
            return new NodePredicate() {
                @Override
                public boolean apply(Node n) {
                    return thiz.apply(n) && np.apply(n);
                }
            };
        }

        public NodePredicate or(final NodePredicate np) {
            final NodePredicate thiz = this;
            return new NodePredicate() {
                @Override
                public boolean apply(Node n) {
                    return thiz.apply(n) || np.apply(n);
                }
            };
        }
    }

    public static final class TypePredicate extends NodePredicate {
        private Class<? extends Node> type;
        public TypePredicate(Class< ? extends Node> type) {
            this.type = type;
        }
        @Override
        public boolean apply(Node n) {
            if (type.isInstance(n)) {
                return true;
            }
            return false;
        }
    }

    public abstract static class NodeIterable<T extends Node> implements Iterable<T> {
        protected NodePredicate until = new NodePredicate() {
            @Override
            public boolean apply(Node n) {
                return n == null;
            }
        };
        public NodeIterable<T> until(final T u) {
            until = until.or(new NodePredicate() {
                @Override
                public boolean apply(Node n) {
                    return u == n;
                }
            });
            return this;
        }
        public NodeIterable<T> until(final Class<? extends T> clazz) {
            until = until.or(new NodePredicate() {
                @Override
                public boolean apply(Node n) {
                    return clazz.isInstance(n);
                }
            });
            return this;
        }
        public <F extends T> FilteredNodeIterable<F> filter(Class<F> clazz) {
            return new FilteredNodeIterable<T>(this).and(clazz);
        }
    }

    public static class FilteredNodeIterable<T extends Node> extends NodeIterable<T> {
        private final NodeIterable<T> nodeIterable;
        private NodePredicate predicate = new NodePredicate() {
            @Override
            public boolean apply(Node n) {
                return true;
            }
        };
        public FilteredNodeIterable(NodeIterable<T> nodeIterable) {
            this.nodeIterable = nodeIterable;
            this.until = nodeIterable.until;
        }
        @SuppressWarnings("unchecked")
        public <F extends T> FilteredNodeIterable<F> and(Class<F> clazz) {
            this.predicate = predicate.and(new TypePredicate(clazz));
            return (FilteredNodeIterable<F>) this;
        }
        @SuppressWarnings("unchecked")
        public FilteredNodeIterable<Node> or(Class<? extends Node> clazz) {
            this.predicate = predicate.or(new TypePredicate(clazz));
            return (FilteredNodeIterable<Node>) this;
        }
        @Override
        public Iterator<T> iterator() {
            final Iterator<T> iterator = nodeIterable.iterator();
            return new PredicatedProxyNodeIterator<T>(until, iterator, predicate);
        }
    }

    public abstract static class NodeIterator<T extends Node> implements Iterator<T>{
        protected T current;
        protected final NodePredicate until;
        public NodeIterator(NodePredicate until) {
            this.until = until;
        }
        protected abstract void forward();
        @Override
        public boolean hasNext() {
            forward();
            return !until.apply(current);
        }
        @Override
        public T next() {
            forward();
            T ret = current;
            if (until.apply(current)) {
                throw new NoSuchElementException();
            }
            current = null;
            return ret;
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static final class PredicatedProxyNodeIterator<T extends Node> extends NodeIterator<T> {
        private final Iterator<T> iterator;
        private final NodePredicate predicate;
        private PredicatedProxyNodeIterator(NodePredicate until, Iterator<T> iterator, NodePredicate predicate) {
            super(until);
            this.iterator = iterator;
            this.predicate = predicate;
        }
        @Override
        protected void forward() {
            while ((current == null || !current.isAlive() || !predicate.apply(current)) && iterator.hasNext()) {
                current = iterator.next();
            }
        }
    }

    public static NodeIterable<FixedNode> dominators(final FixedNode n) {
        return new NodeIterable<FixedNode>() {
            @Override
            public Iterator<FixedNode> iterator() {
                return new NodeIterator<FixedNode>(until){
                    FixedNode p = n;
                    @Override
                    protected void forward() {
                        if (current == null) {
                            if (p instanceof MergeNode) {
                                current = new ComputeImmediateDominator((MergeNode) p).compute();
                            } else {
                                current = (FixedNode) p.predecessor();
                            }
                            p = current;
                        }
                    }
                };
            }
        };
    }
}
