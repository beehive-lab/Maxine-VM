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
/*VCSID=61c465be-328d-4985-a4d0-3ab5b361c84d*/
package com.sun.max.asm;

import com.sun.max.lang.*;

/**
 * This class provides a skeletal implementation of the {@link SymbolicArgument} interface, to minimize the
 * effort required to implement this interface.
 *
 * @author Doug Simon
 */
public abstract class AbstractSymbolicArgument implements SymbolicArgument, StaticFieldName {

    private String _name;
    private final int _value;

    protected AbstractSymbolicArgument(String name, int value) {
        _name = name;
        _value = value;
    }

    protected AbstractSymbolicArgument(int value) {
        _value = value;
    }

    public void setName(String name) {
        _name = name;
    }

    public String name() {
        return _name;
    }

    public int value() {
        return _value;
    }

    public String externalValue() {
        return "%" + name().toLowerCase();
    }

    public long asLong() {
        return value();
    }

    public String disassembledValue() {
        return externalValue();
    }

    @Override
    public String toString() {
        return name();
    }
}
