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
/*VCSID=5fd6b9a9-14f4-48ed-aa71-d65305bcaa34*/
package com.sun.max.vm.classfile.constant;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.classfile.constant.SymbolTable.*;

/**
 * Canonical representation of strings.
 * 
 * #4.4.7.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Utf8Constant extends AbstractPoolConstant<Utf8Constant> implements PoolConstantKey<Utf8Constant>, Comparable<Utf8Constant> {

    @Override
    public final Tag tag() {
        return Tag.UTF8;
    }

    @INSPECTED
    private final String _string;

    public final String string() {
        return _string;
    }

    /**
     * Must only be called from {@link Utf8ConstantEntry#Utf8ConstantEntry(String)}.
     */
    Utf8Constant(String value) {
        // Can only be subclassed by Utf8ConstantEntry
        assert getClass() == Utf8ConstantEntry.class;
        _string = value;
    }

    @Override
    public final String toString() {
        return _string;
    }

    public final String valueString(ConstantPool pool) {
        return _string;
    }

    @Override
    public final boolean equals(Object other) {
        return this == other;
    }

    public final boolean equals(String string) {
        return _string.equals(string);
    }

    @Override
    public final int hashCode() {
        return _string.hashCode();
    }

    @Override
    public final Utf8Constant key(ConstantPool pool) {
        return this;
    }

    public final int compareTo(Utf8Constant other) {
        return _string.compareTo(other._string);
    }
}
