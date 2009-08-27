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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * Interface denoting a field or method entry in a constant pool.
 *
 * @author Doug Simon
 */
public interface MemberRefConstant<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends ResolvableConstant<PoolConstant_Type, MemberActor> {

    /**
     * Gets the descriptor of the class (i.e. holder) in which this method or field is declared. Note that the actual
     * holder after resolution may be a super class of the class described by the descriptor returned by this method.
     *
     * @param pool the constant pool that maybe be required to convert a constant pool index to the holder's descriptor
     */
    TypeDescriptor holder(ConstantPool pool);

    /**
     * Gets the name of this field or method.
     *
     * @param pool the constant pool that maybe be required to convert a constant pool index to a name
     */
    Utf8Constant name(ConstantPool pool);

    /**
     * Gets the type of this field or signature of this method.
     *
     * @param pool the constant pool that maybe be required to convert a constant pool index to this field/method's type/signature.
     */
    Descriptor descriptor(ConstantPool pool);
}
