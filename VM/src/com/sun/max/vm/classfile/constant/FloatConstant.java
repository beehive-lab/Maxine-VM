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
/*VCSID=8196cc5b-f4c6-4091-945e-504ec71b2540*/
package com.sun.max.vm.classfile.constant;

import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.value.*;

/**
 * #4.4.4.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class FloatConstant extends AbstractPoolConstant<FloatConstant> implements PoolConstantKey<FloatConstant>, ValueConstant {

    @Override
    public Tag tag() {
        return Tag.FLOAT;
    }

    private final float _value;

    FloatConstant(float value) {
        _value = value;
    }

    public float value() {
        return _value;
    }

    public String valueString(ConstantPool pool) {
        return String.valueOf(_value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FloatConstant) {
            final FloatConstant key = (FloatConstant) other;
            return _value == key._value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) _value;
    }

    public Value value(ConstantPool pool, int index) {
        return FloatValue.from(_value);
    }

    @Override
    public FloatConstant key(ConstantPool pool) {
        return this;
    }
}
