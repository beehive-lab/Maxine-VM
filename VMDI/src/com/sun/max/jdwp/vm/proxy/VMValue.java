/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
