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
/*VCSID=1bec5c79-a2e9-4bb3-907c-3cad97f03d80*/
package com.sun.max.collect;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;

/**
 * Lisp-like lists made of "cons" cells.
 *
 * @author Bernd Mathiske
 * 
 * Notes:  the representation of the empty list is null,
 * which means that the non-static methods here do not apply
 * 
 */
public class Cons<Element_Type> implements LinearCollection<Element_Type> {

    private final Element_Type _head;
    private final Cons<Element_Type> _tail;

    public Cons(Element_Type head, Cons<Element_Type> tail) {
        _head = head;
        _tail = tail;
    }

    public Element_Type head() {
        return _head;
    }

    public Cons<Element_Type> tail() {
        return _tail;
    }

    public int length() {
        final Cons<Element_Type> start = this;
        Cons<Element_Type> list = this;
        int n = 0;
        do {
            n++;
            list = list.tail();
        } while (list != null && list != start);
        return n;
    }

    public Iterator<Element_Type> iterator() {
        @JdtSyntax("Workaround for type checker bug:")
        final Class<Cons<Element_Type>> type = null;
        return new Iterator<Element_Type>() {
            private final Cons<Element_Type> _start = StaticLoophole.cast(type, Cons.this);
            private Cons<Element_Type> _list = _start;

            public boolean hasNext() {
                return _list != null;
            }
            public Element_Type next() {
                if (_list == null) {
                    throw new NoSuchElementException();
                }
                final Element_Type result = _list.head();
                _list = _list.tail();
                if (_list == _start) {
                    _list = null;
                }
                return result;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <Element_Type> Cons<Element_Type> create(Iterable<Element_Type> elements) {
        return createReverse(createReverse(elements));
    }

    public static <Element_Type> Cons<Element_Type> createReverse(Iterable<Element_Type> elements) {
        if (elements == null) {
            return null;
        }
        Cons<Element_Type> list = null;
        for (Element_Type element : elements) {
            list = new Cons<Element_Type>(element, list);
        }
        return list;
    }

    public static <Element_Type> boolean equals(Cons<Element_Type> aCons, Cons<Element_Type> bCons) {
        Cons<Element_Type> a = aCons;
        Cons<Element_Type> b = bCons;
        while (a != null && b != null) {
            if (!(a.head().equals(b.head()))) {
                return false;
            }
            a = a.tail();
            b = b.tail();
        }
        return a == b;
    }

}
