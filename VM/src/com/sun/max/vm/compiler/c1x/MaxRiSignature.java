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
import com.sun.c1x.value.*;
import com.sun.max.vm.type.*;

/**
<<<<<<< local
 * The <code>MaxCiSignature</code> class implements a method signature for the
=======
 * The <code>MaxRiSignature</code> class implements a method signature for the
>>>>>>> other
 * compiler interface.
 *
 * @author Ben L. Titzer
 */
public class MaxRiSignature implements RiSignature {

    final MaxRiConstantPool constantPool;
    final SignatureDescriptor descriptor;
    BasicType[] basicTypes;
    BasicType basicReturnType;
<<<<<<< local
    MaxRiType[] ciTypes;
=======
    MaxRiType[] riTypes;
>>>>>>> other
    MaxRiType ciReturnType;

    /**
     * Creates a new signature within the specified constant pool from the specified signature descriptor.
     * @param constantPool the constant pool used to canonicalize types
     * @param descriptor the signature descriptor
     */
    public MaxRiSignature(MaxRiConstantPool constantPool, SignatureDescriptor descriptor) {
        this.constantPool = constantPool;
        this.descriptor = descriptor;
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
<<<<<<< local
        if (ciTypes == null) {
=======
        if (riTypes == null) {
>>>>>>> other
            final int max = descriptor.numberOfParameters();
<<<<<<< local
            ciTypes = new MaxRiType[max];
=======
            riTypes = new MaxRiType[max];
>>>>>>> other
            for (int i = 0; i < max; i++) {
<<<<<<< local
                ciTypes[i] = descriptorToCiType(descriptor.parameterDescriptorAt(i));
=======
                riTypes[i] = descriptorToRiType(descriptor.parameterDescriptorAt(i));
>>>>>>> other
            }

        }
<<<<<<< local
        return ciTypes[index];
=======
        return riTypes[index];
>>>>>>> other
    }

    /**
     * Gets the basic type of the specified argument.
     * This is typically implemented more efficiently than getting the actual type.
     * @param index the index of the argument
     * @return the basic type of the argument
     */
    public BasicType argumentBasicTypeAt(int index) {
        if (basicTypes == null) {
            final int max = descriptor.numberOfParameters();
            basicTypes = new BasicType[max];
            for (int i = 0; i < max; i++) {
                basicTypes[i] = descriptorToBasicType(descriptor.parameterDescriptorAt(i));
            }
        }
        return basicTypes[index];
    }

    /**
     * Gets the return type of this signature.
     * @return the return type
     */
    public RiType returnType() {
        if (ciReturnType == null) {
<<<<<<< local
            ciReturnType = descriptorToCiType(descriptor.resultDescriptor());
=======
            ciReturnType = descriptorToRiType(descriptor.resultDescriptor());
>>>>>>> other
        }
        return ciReturnType;
    }

    /**
     * Gets the basic return type of this signature.
     * This is typically implemented more efficiently than getting the actual type.
     * @return the basic return type
     */
    public BasicType returnBasicType() {
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
     * @param withReceiver <code>true</code> if a receiver argument should be added; <code>false</code> otherwise
     * @return the size in slots of the arguments to this signature
     */
    public int argumentSlots(boolean withReceiver) {
        // XXX: cache the argument size
        return descriptor.computeNumberOfSlots() + (withReceiver ? 1 : 0);
    }

    private BasicType descriptorToBasicType(TypeDescriptor typeDescriptor) {
        return MaxRiType.kindToBasicType(typeDescriptor.toKind());
    }

<<<<<<< local
    private MaxRiType descriptorToCiType(TypeDescriptor typeDescriptor) {
=======
    private MaxRiType descriptorToRiType(TypeDescriptor typeDescriptor) {
>>>>>>> other
         // TODO: resolve the descriptor if possible in the constant pool
        return new MaxRiType(constantPool, typeDescriptor);
    }

}
