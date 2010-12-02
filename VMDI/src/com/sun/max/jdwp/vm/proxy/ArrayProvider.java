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

/**
 * An interface that allows accessing an array object in the VM.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface ArrayProvider extends ObjectProvider {

    /**
     * Allows access to the actual length of the array as stored in the VM.
     *
     * @return the length of the array
     */
    int length();

    /**
     * Allows access to an element of the array. The return value is given as wrapped in a VMValue object.
     *
     * @param i index of the accessed array element
     * @return the value of the array at the specified index
     */
    VMValue getValue(int i);

    /**
     * Sets the element of the array.
     *
     * @param i index of the element to set
     * @param value the value to set at the specified index
     */
    void setValue(int i, VMValue value);

    /**
     * The type of an array is always of type ArrayTypeProvider.
     *
     * @return the type of the array
     */
    ArrayTypeProvider getArrayType();
}
