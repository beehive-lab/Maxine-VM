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
package com.sun.max.util;

import java.util.*;

import com.sun.max.program.*;


/**
 * An array-based recording of the history of a value, with
 * time expressed as the number of generations back from the current generation (0).
 *
 * @author Michael Van De Vanter
 */
public class ArrayValueHistory<Value_Type> implements ValueHistory<Value_Type> {

    private final ArrayDeque<Value_Type> _generations;
    private final int _limit;
    private int _age = -1;

    public ArrayValueHistory(int limit) {
        _generations = new ArrayDeque<Value_Type>();
        _limit = limit;
    }

    public ArrayValueHistory() {
        this (Integer.MAX_VALUE);
    }

    public void add(Value_Type newValue) {
        if (_generations.size() > 0) {
            if (newValue.equals(_generations.getFirst())) {
                if (_age >= 0) {
                    _age++;
                }
            } else {
                _age = 0;
            }
        }
        _generations.addFirst(newValue);
        if (_generations.size() > _limit) {
            _generations.removeLast();
        }
    }

    public Value_Type get() {
        if (_generations.size() > 0) {
            return _generations.getFirst();
        }
        ProgramError.unexpected("empty history");
        return null;
    }

    public Value_Type get(int generation) {
        final Iterator<Value_Type> iterator = _generations.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            if (index == generation) {
                return iterator.next();
            }
            index++;
        }
        ProgramError.unexpected("exceeded history");
        return null;
    }

    public int getAge() {
        return _age;
    }

    public int getLimit() {
        return _limit;
    }

    public int getSize() {
        return _generations.size();
    }

    public Iterator<Value_Type> values() {
        return _generations.iterator();
    }

}
