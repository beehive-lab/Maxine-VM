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
/*VCSID=647382de-e5bf-41eb-8d10-151a25677a8a*/
package com.sun.max.collect;

import com.sun.max.lang.*;

/**
 * Unordered pair of elements. Takes no precautions to detect cycles when computing {@link #hashCode()}s from its
 * elements.
 *
 * @author Michael Bebenitia
 */
public class Pair<First_Type, Second_Type> {

    public static <First_Type, Second_Type> Pair<First_Type, Second_Type> from(First_Type kind, Second_Type operation) {
        return new Pair<First_Type, Second_Type>(kind, operation);
    }

    private First_Type _first;
    private Second_Type _second;

    public First_Type first() {
        return _first;
    }

    public Second_Type second() {
        return _second;
    }

    public Pair(First_Type first, Second_Type second) {
        this._first = first;
        this._second = second;
    }

    @Override
    public int hashCode() {
        return _first.hashCode() ^ _second.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        final Pair<First_Type, Second_Type> other = StaticLoophole.cast(obj);
        return (_first == other._first || _first.equals(other._first)) && (_second == other._second || _second.equals(other._second));
    }
}
