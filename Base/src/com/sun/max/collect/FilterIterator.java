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
/*VCSID=8cd511f0-7ff0-4f22-aeff-a62f321fcb33*/
package com.sun.max.collect;

import java.util.*;

import com.sun.max.util.*;

/**
 * Filters the elements returned by a given iterator with a given predicate.
 *
 * @author Doug Simon
 */
public class FilterIterator<Element_Type> implements Iterator<Element_Type> {

    private final Iterator<? extends Element_Type> _iterator;
    private final Predicate<Element_Type> _predicate;
    private Element_Type _next;
    private boolean _advanced;

    public FilterIterator(Iterator<? extends Element_Type> iterator, Predicate<Element_Type> predicate) {
        _iterator = iterator;
        _predicate = predicate;
    }

    public boolean hasNext() {
        if (_advanced) {
            return true;
        }
        return advance();
    }

    public Element_Type next() {
        if (!_advanced) {
            if (!advance()) {
                throw new NoSuchElementException();
            }
        }
        _advanced = false;
        return _next;
    }

    public void remove() {
        if (_advanced) {
            throw new IllegalStateException("remove() cannot be called");
        }
        _iterator.remove();
    }

    private boolean advance() {
        while (_iterator.hasNext()) {
            final Element_Type next = _iterator.next();
            if (_predicate.evaluate(next)) {
                _next = next;
                _advanced = true;
                return true;
            }
        }
        return false;
    }
}
