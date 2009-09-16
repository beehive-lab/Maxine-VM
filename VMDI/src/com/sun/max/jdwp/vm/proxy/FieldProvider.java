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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Interface to a field in the VM.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface FieldProvider extends Provider {

    /**
     * Only valid when it is a static field.
     *
     * @return the static value of the field
     */
    VMValue getStaticValue();

    /**
     * Only valid when it is a static field. Sets the static value of a field.
     *
     * @param value the value that should be the new value of the field
     */
    void setStaticValue(VMValue value);

    /**
     * Accesses the value of the field for a specific instance object.
     *
     * @param object the instance object whose field value to access
     * @return the value of the field
     */
    VMValue getValue(ObjectProvider object);

    /**
     * Sets the value of a field for a specific instance object.
     *
     * @param object the instance object whose field should be set
     * @param value the value that should be the new value of the field
     */
    void setValue(ObjectProvider object, VMValue value);

    /**
     * The enclosing reference type of the field. For instance fields this can only be a class type.
     *
     * @return the enclosing reference type
     */
    ReferenceTypeProvider getReferenceTypeHolder();

    /**
     * @return the type of the field
     */
    VMValue.Type getType();

    /**
     * @return the flags of the field according to the Java VM specification
     */
    int getFlags();

    /**
     * @return the name of the field
     */
    String getName();

    /**
     * @return the signature of the field according to the Java VM specification
     */
    String getSignature();

    /**
     * @return the generic signature of the field according to the Java VM specification
     */
    String getGenericSignature();
}
