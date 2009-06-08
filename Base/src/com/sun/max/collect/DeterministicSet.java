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
package com.sun.max.collect;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * A set that always iterates over its elements in the order in which they have been added.
 *
 * @author Bernd Mathiske
 */
public interface DeterministicSet<Element_Type> extends Sequence<Element_Type> {

    boolean contains(Element_Type element);

    int length();

    Element_Type getOne();

    public static final class Static {
        private Static() {
        }

        private static final class Empty<Element_Type> implements DeterministicSet<Element_Type> {

            public Empty() {
            }

            public boolean contains(Element_Type element) {
                return false;
            }

            public int size() {
                return 0;
            }

            public Element_Type getOne() {
                return first();
            }

            public boolean isEmpty() {
                return true;
            }

            public int length() {
                return 0;
            }

            public Element_Type first() {
                throw new NoSuchElementException();
            }

            public Element_Type last() {
                throw new NoSuchElementException();
            }

            @Override
            public Empty<Element_Type> clone() {
                try {
                    return StaticLoophole.cast(super.clone());
                } catch (CloneNotSupportedException e) {
                    ProgramError.unexpected();
                    return null;
                }
            }

            public Collection<Element_Type> toCollection() {
                return Collections.emptySet();
            }

            public Iterator<Element_Type> iterator() {
                return new Iterator<Element_Type>() {
                    public boolean hasNext() {
                        return false;
                    }

                    public Element_Type next() {
                        throw new NoSuchElementException();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        private static final DeterministicSet EMPTY = new Empty();

        public static <Element_Type> DeterministicSet<Element_Type> empty(Class<Element_Type> elementType) {
            final Class<DeterministicSet<Element_Type>> sequenceType = null;
            return StaticLoophole.cast(sequenceType, EMPTY);
        }
    }

    public static final class Singleton<Element_Type> implements DeterministicSet<Element_Type> {
        private final Element_Type _element;

        public Singleton(Element_Type element) {
            _element = element;
        }

        public boolean contains(Element_Type element) {
            return element == _element;
        }

        public int size() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }

        public int length() {
            return 1;
        }

        public Element_Type first() {
            return _element;
        }

        public Element_Type getOne() {
            return first();
        }

        public Element_Type last() {
            return _element;
        }

        @Override
        public Singleton<Element_Type> clone() {
            try {
                return StaticLoophole.cast(super.clone());
            } catch (CloneNotSupportedException e) {
                ProgramError.unexpected();
                return null;
            }
        }

        public Collection<Element_Type> toCollection() {
            return Collections.singleton(_element);
        }

        public Iterator<Element_Type> iterator() {
            return new Iterator<Element_Type>() {
                private boolean _isDone;

                public boolean hasNext() {
                    return !_isDone;
                }

                public Element_Type next() {
                    if (_isDone) {
                        throw new NoSuchElementException();
                    }
                    _isDone = true;
                    return _element;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
