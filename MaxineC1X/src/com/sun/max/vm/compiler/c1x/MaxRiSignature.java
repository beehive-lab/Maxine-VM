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

import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.max.vm.type.*;

/**
 * The {@code MaxRiSignature} class implements a method signature for the
 * compiler interface.
 *
 * @author Ben L. Titzer
 */
public class MaxRiSignature implements RiSignature {

    final MaxRiConstantPool constantPool;
    final SignatureDescriptor descriptor;
    int argSlots = -1;
    final CiKind[] basicTypes;
    CiKind basicReturnType;
    final MaxRiType[] riTypes;
    MaxRiType ciReturnType;

    /**
     * Creates a new signature within the specified constant pool from the specified signature descriptor.
     * @param constantPool the constant pool used to canonicalize types
     * @param descriptor the signature descriptor
     */
    public MaxRiSignature(MaxRiConstantPool constantPool, SignatureDescriptor descriptor) {
        this.constantPool = constantPool;
        this.descriptor = descriptor;
        final int max = descriptor.numberOfParameters();

        basicTypes = new CiKind[max];
        for (int i = 0; i < max; i++) {
            basicTypes[i] = descriptorToBasicType(descriptor.parameterDescriptorAt(i));
        }

        final int numberOfParameters = descriptor.numberOfParameters();

        riTypes = new MaxRiType[numberOfParameters];
        for (int i = 0; i < numberOfParameters; i++) {
            riTypes[i] = descriptorToRiType(descriptor.parameterDescriptorAt(i));
        }
    }

    /**
     * Gets the number of arguments in this signature (not including receiver).
     *
     * @return the number of arguments
     */
    public int argumentCount(boolean receiver) {
        return descriptor.numberOfParameters() + (receiver ? 1 : 0);
    }

    /**
     * Gets the type of the specified argument.
     * @param index the index of the argument
     * @return the type of the specified argument
     */
    public RiType argumentTypeAt(int index) {
        return riTypes[index];
    }

    /**
     * Gets the kind of the specified argument.
     * This is typically implemented more efficiently than getting the actual type.
     * @param index the index of the argument
     * @return the kind of the argument
     */
    public CiKind argumentKindAt(int index) {
        return basicTypes[index];
    }

    /**
     * Gets the return type of this signature.
     * @return the return type
     */
    public RiType returnType() {
        if (ciReturnType == null) {
            ciReturnType = descriptorToRiType(descriptor.resultDescriptor());
        }
        return ciReturnType;
    }

    /**
     * Gets the basic return type of this signature.
     * This is typically implemented more efficiently than getting the actual type.
     * @return the basic return type
     */
    public CiKind returnKind() {
        if (basicReturnType == null) {
            basicReturnType = descriptorToBasicType(descriptor.resultDescriptor());
        }
        return basicReturnType;
    }

    /**
     * Converts this signature to a string.
     * @return the string representation of this signature
     */
    public String asString() {
        return descriptor.toString();
    }

    @Override
    public String toString() {
        return asString();
    }

    /**
     * Gets the size (in terms of Java slots) of the arguments, with or without a receiver object.
     * @param withReceiver {@code true} if a receiver argument should be added; {@code false} otherwise
     * @return the size in slots of the arguments to this signature
     */
    public int argumentSlots(boolean withReceiver) {
        if (argSlots == -1) {
            argSlots = descriptor.computeNumberOfSlots();
        }
        return argSlots + (withReceiver ? 1 : 0);
    }

    private CiKind descriptorToBasicType(TypeDescriptor typeDescriptor) {
        return MaxRiType.kindToBasicType(typeDescriptor.toKind());
    }

    private MaxRiType descriptorToRiType(TypeDescriptor typeDescriptor) {
         // TODO: resolve the descriptor if possible in the constant pool
        return new MaxRiType(constantPool, typeDescriptor, -1);
    }

}
