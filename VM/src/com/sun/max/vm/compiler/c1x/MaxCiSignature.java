/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import com.sun.c1x.ci.CiSignature;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.BasicType;
import com.sun.max.vm.type.SignatureDescriptor;
import com.sun.max.vm.type.TypeDescriptor;

/**
 * The <code>MaxCiSignature</code> class implements a method signature for the
 * compiler interface.
 *
 * @author Ben L. Titzer
 */
public class MaxCiSignature implements CiSignature {

    final MaxCiConstantPool _constantPool;
    final SignatureDescriptor _descriptor;
    BasicType[] _basicTypes;
    BasicType _basicReturnType;
    MaxCiType[] _ciTypes;
    MaxCiType _ciReturnType;

    /**
     * Creates a new signature within the specified constant pool from the specified signature descriptor.
     * @param constantPool the constant pool used to canonicalize types
     * @param descriptor the signature descriptor
     */
    public MaxCiSignature(MaxCiConstantPool constantPool, SignatureDescriptor descriptor) {
        _constantPool = constantPool;
        _descriptor = descriptor;
    }

    /**
     * Gets the number of arguments in this signature (not including receiver).
     * @return the number of arguments
     */
    public int arguments() {
        return _descriptor.numberOfParameters();
    }

    /**
     * Gets the type of the specified argument.
     * @param index the index of the argument
     * @return the type of the specified argument
     */
    public CiType argumentType(int index) {
        if (_ciTypes == null) {
            final int max = _descriptor.numberOfParameters();
            _ciTypes = new MaxCiType[max];
            for (int i = 0; i < max; i++) {
                _ciTypes[i] = descriptorToCiType(_descriptor.parameterDescriptorAt(i));
            }

        }
        return _ciTypes[index];
    }

    /**
     * Gets the basic type of the specified argument.
     * This is typically implemented more efficiently than getting the actual type.
     * @param index the index of the argument
     * @return the basic type of the argument
     */
    public BasicType argumentBasicType(int index) {
        if (_basicTypes == null) {
            final int max = _descriptor.numberOfParameters();
            _basicTypes = new BasicType[max];
            for (int i = 0; i < max; i++) {
                _basicTypes[i] = descriptorToBasicType(_descriptor.parameterDescriptorAt(i));
            }
        }
        return _basicTypes[index];
    }

    /**
     * Gets the return type of this signature.
     * @return the return type
     */
    public CiType returnType() {
        if (_ciReturnType == null) {
            _ciReturnType = descriptorToCiType(_descriptor.resultDescriptor());
        }
        return _ciReturnType;
    }

    /**
     * Gets the basic return type of this signature.
     * This is typically implemented more efficiently than getting the actual type.
     * @return the basic return type
     */
    public BasicType returnBasicType() {
        if (_basicReturnType == null) {
            _basicReturnType = descriptorToBasicType(_descriptor.resultDescriptor());
        }
        return _basicReturnType;
    }

    /**
     * Converts this signature to a string.
     * @return the string representation of this signature
     */
    public String asString() {
        return _descriptor.toString();
    }

    @Override
    public String toString() {
        return asString();
    }

    /**
     * Gets the size (in terms of Java slots) of the arguments, with or without a receiver object.
     * @param withReceiver <code>true</code> if a receiver argument should be added; <code>false</code> otherwise
     * @return the size in slots of the arguments to this signature
     */
    public int argumentSize(boolean withReceiver) {
        // XXX: cache the argument size
        return _descriptor.computeNumberOfSlots() + (withReceiver ? 1 : 0);
    }

    private BasicType descriptorToBasicType(TypeDescriptor descriptor) {
        return MaxCiType.kindToBasicType(descriptor.toKind());
    }

    private MaxCiType descriptorToCiType(TypeDescriptor descriptor) {
         // TODO: resolve the descriptor if possible in the constant pool
        return new MaxCiType(_constantPool, descriptor);
    }

}
