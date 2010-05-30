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

import com.sun.max.util.*;

/**
 * Filters the elements returned by a given iterator with a given predicate.
 *
 * @author Doug Simon
 */
public class FilterIterator<Element_Type> implements Iterator<Element_Type> {

    private final Iterator<? extends Element_Type> iterator;
    private final Predicate<Element_Type> predicate;
    private Element_Type next;
    private boolean advanced;

    public FilterIterator(Iterator<? extends Element_Type> iterator, Predicate<Element_Type> predicate) {
        this.iterator = iterator;
        this.predicate = predicate;
    }

    public boolean hasNext() {
        if (advanced) {
            return true;
        }
        return advance();
    }

    public Element_Type next() {
        if (!advanced) {
            if (!advance()) {
                throw new NoSuchElementException();
            }
        }
        advanced = false;
        return next;
    }

    public void remove() {
        if (advanced) {
            throw new IllegalStateException("remove() cannot be called");
        }
        iterator.remove();
    }

    private boolean advance() {
        while (iterator.hasNext()) {
            final Element_Type n = iterator.next();
            if (predicate.evaluate(n)) {
                next = n;
                advanced = true;
                return true;
            }
        }
        return false;
    }
}
