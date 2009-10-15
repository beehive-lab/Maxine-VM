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
 * Represents a single value in the VM.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface VMValue {

    /**
     * Enumeration of all possible types of a JDWP value.
     */
    public enum Type {
        VOID,
        BYTE,
        CHAR,
        SHORT,
        INT,
        FLOAT,
        DOUBLE,
        LONG,
        BOOLEAN,
        PROVIDER
    };

    /**
     * @return true if this value is a void value, false otherwise
     */
    boolean isVoid();

    /**
     * @return the value as a byte value or null if it has a different type
     */
    Byte asByte();

    /**
     * @return the value as a char value or null if it has a different type
     */
    Character asChar();

    /**
     *
     * @return the value as a short value or null if it has a different type
     */
    Short asShort();

    /**
     * @return the value as an integer value or null if it has a different type
     */
    Integer asInt();

    /**
     * @return the value as a float value or null if it has a different type
     */
    Float asFloat();

    /**
     * @return the value as a double value or null if it has a different type
     */
    Double asDouble();

    /**
     * @return the value as a long value or null if it has a different type
     */
    Long asLong();

    /**
     * @return the value as a boolean value or null if it has a different type
     */
    Boolean asBoolean();

    /**
     * @return the value as a provider object or null if it has a different type
     */
    Provider asProvider();

    /**
     * This method may only be called if the value is not of type void.
     *
     * @return the Java object that is represented by this VMValue object
     */
    Object asJavaObject();
}
