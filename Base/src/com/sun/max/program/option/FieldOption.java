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
package com.sun.max.program.option;

import com.sun.max.lang.StaticLoophole;
import com.sun.max.program.ProgramError;

import java.lang.reflect.Field;

/**
 * This class implements a command line option that stores its value in a field
 * via reflection.
 *
 * @author Ben L. Titzer
 */
public class FieldOption<Value_Type> extends Option<Value_Type> {

    protected final Field field;

    public FieldOption(String name, Field field, Value_Type defaultValue, Type<Value_Type> type, String help) {
        super(name, defaultValue, type, help);
        this.field = field;
    }

    /**
     * Gets the value of this option. This implementation stores the field's value in a reflected field
     * and access requires a reflective access.
     * @return the value of this option
     */
    @Override
    public Value_Type getValue() {
        try {
            return StaticLoophole.cast(field.get(null));
        } catch (IllegalAccessException e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * Sets the value of this option. This implementation stores the field's value in a reflected field
     * and thus setting the value requires a reflective access.
     * @param value the value to set the new value to
     */
    @Override
    public void setValue(Value_Type value) {
        try {
            field.set(null, value);
        } catch (Exception e) {
            throw ProgramError.unexpected("Error updating the value of " + field, e);
        }
    }
}
