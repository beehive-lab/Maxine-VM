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
package com.sun.max.vm.classfile.constant;

import static com.sun.max.vm.classfile.ErrorContext.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

/**
 * #4.4.6.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class NameAndTypeConstant extends AbstractPoolConstant<NameAndTypeConstant> implements PoolConstantKey<NameAndTypeConstant> {

    private final Utf8Constant _name;

    private Object _descriptor;

    @Override
    public Tag tag() {
        return Tag.NAME_AND_TYPE;
    }

    NameAndTypeConstant(Utf8Constant name, Utf8Constant descriptor) {
        _name = name;
        _descriptor = descriptor;
    }

    public NameAndTypeConstant(Utf8Constant name, Descriptor descriptor) {
        _name = name;
        _descriptor = descriptor;
    }

    public Utf8Constant name() {
        return _name;
    }

    public String descriptorString() {
        return _descriptor.toString();
    }

    public TypeDescriptor type() {
        if (_descriptor instanceof Utf8Constant) {
            try {
                _descriptor = JavaTypeDescriptor.parseTypeDescriptor(((Utf8Constant) _descriptor).toString());
            } catch (ClassCastException e) {
                // This just means another thread beats us in the race to convert _descriptor to a real Descriptor object.
                // If _descriptor still has the wrong Descriptor type, then the cast in the return statement will catch it.
                // Using an exception handler obviates the need for synchronization.
            }
        }
        try {
            return (TypeDescriptor) _descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(_descriptor + " is not a valid field type descriptor");
        }
    }

    public SignatureDescriptor signature() {
        if (_descriptor instanceof Utf8Constant) {
            try {
                _descriptor = SignatureDescriptor.create(((Utf8Constant) _descriptor).toString());
            } catch (ClassCastException e) {
                // This just means another thread beats us in the race to convert _descriptor to a real Descriptor object.
                // If _descriptor still has the wrong Descriptor type, then the following cast will catch it.
                // Using an exception handler obviates the need for synchronization.
            }

        }
        try {
            return (SignatureDescriptor) _descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(_descriptor + " is not a valid method signature descriptor");
        }
    }


    public Descriptor descriptor() {
        if (_descriptor instanceof Descriptor) {
            return (Descriptor) _descriptor;
        }
        if (descriptorString().charAt(0) == '(') {
            return signature();
        }
        return type();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NameAndTypeConstant) {
            final NameAndTypeConstant nameAndType = (NameAndTypeConstant) object;
            return _name.equals(nameAndType._name) && (_descriptor == nameAndType._descriptor || descriptorString().equals(nameAndType.descriptorString()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _name.hashCode() ^ descriptorString().hashCode();
    }

    @Override
    public NameAndTypeConstant key(ConstantPool pool) {
        return this;
    }

    public String valueString(ConstantPool pool) {
        return "name=\"" + name() + "\",descriptor=\"" + _descriptor + "\"";
    }
}
