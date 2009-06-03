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
package com.sun.max.vm.object.host;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.prototype.JDKInterceptor.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * This class implements utilities that provide access to host objects that represent
 * classes (i.e. not arrays or hybrids).
 *
 * @author Bernd Mathiske
 */
@PROTOTYPE_ONLY
public final class HostTupleAccess {
    private HostTupleAccess() {
    }

    /**
     * Generates an error that the field could not be accessed.
     * @param fieldActor the field actor that caused the problem
     */
    private static void accessError(FieldActor fieldActor) {
        ProgramError.unexpected("could not access field: " + fieldActor);
    }

    /**
     * Reads the value of a field in the specified object.
     * @param tuple the object that contains the field
     * @param fieldActor the field to read from the object
     * @return the internal boxed representation of the value of the field
     */
    public static Value readValue(Object tuple, FieldActor fieldActor) {
        if (fieldActor.isInjected()) {
            final InjectedFieldActor injectedFieldActor = (InjectedFieldActor) fieldActor;
            return injectedFieldActor.readInjectedValue(Reference.fromJava(tuple));
        }
        // is this an intercepted field?
        final InterceptedField interceptedField = JDKInterceptor.getInterceptedField(fieldActor);
        if (interceptedField != null) {
            return interceptedField.getValue(tuple, fieldActor);
        }
        // does the field have a constant value?
        final Value constantValue = fieldActor.constantValue();
        if (constantValue != null) {
            return constantValue;
        }
        // try to read the field's value via reflection
        try {
            final Field field = fieldActor.toJava();
            field.setAccessible(true);
            Object boxedJavaValue = field.get(tuple);
            if (fieldActor.kind() == Kind.REFERENCE) {
                boxedJavaValue = HostObjectAccess.hostToTarget(boxedJavaValue);
            }
            return fieldActor.kind().asValue(boxedJavaValue);
        } catch (IllegalAccessException illegalAcessexception) {
            accessError(fieldActor);
            return null;
        }
    }

    /**
     * Read a byte from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param byteFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static byte readByte(Object tuple, ByteFieldActor byteFieldActor) {
        try {
            final Field field = byteFieldActor.toJava();
            field.setAccessible(true);
            return field.getByte(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(byteFieldActor);
            return (byte) 0;
        }
    }

    /**
     * Read a boolean from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param booleanFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static boolean readBoolean(Object tuple, BooleanFieldActor booleanFieldActor) {
        try {
            final Field field = booleanFieldActor.toJava();
            field.setAccessible(true);
            return field.getBoolean(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(booleanFieldActor);
            return false;
        }
    }

    /**
     * Read a short from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param shortFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static short readShort(Object tuple, ShortFieldActor shortFieldActor) {
        try {
            final Field field = shortFieldActor.toJava();
            field.setAccessible(true);
            return field.getShort(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(shortFieldActor);
            return (short) 0;
        }
    }

    /**
     * Read a char from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param charFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static char readChar(Object tuple, CharFieldActor charFieldActor) {
        try {
            final Field field = charFieldActor.toJava();
            field.setAccessible(true);
            return field.getChar(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(charFieldActor);
            return '\0';
        }
    }

    /**
     * Read an int from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param intFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static int readInt(Object tuple, IntFieldActor intFieldActor) {
        try {
            final Field field = intFieldActor.toJava();
            field.setAccessible(true);
            return field.getInt(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(intFieldActor);
            return 0;
        }
    }

    /**
     * Read a float from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param floatFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static float readFloat(Object tuple, FloatFieldActor floatFieldActor) {
        try {
            final Field field = floatFieldActor.toJava();
            field.setAccessible(true);
            return field.getFloat(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(floatFieldActor);
            return (float) 0.0;
        }
    }

    /**
     * Read a long from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param longFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static long readLong(Object tuple, LongFieldActor longFieldActor) {
        try {
            final Field field = longFieldActor.toJava();
            field.setAccessible(true);
            return field.getLong(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(longFieldActor);
            return 0L;
        }
    }

    /**
     * Read a double from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param doubleFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static double readDouble(Object tuple, DoubleFieldActor doubleFieldActor) {
        try {
            final Field field = doubleFieldActor.toJava();
            field.setAccessible(true);
            return field.getDouble(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(doubleFieldActor);
            return 0.0;
        }
    }

    /**
     * Read a word from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param wordFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static Word readWord(Object tuple, WordFieldActor wordFieldActor) {
        try {
            final Field field = wordFieldActor.toJava();
            field.setAccessible(true);
            return (Word) field.get(tuple);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(wordFieldActor);
            return Word.zero();
        }
    }

    /**
     * Read a reference from the specified field in the specified object.
     * @param tuple the object from which to read the field
     * @param referenceFieldActor the field actor representing the field to be read
     * @return the value of the field of the specified object
     */
    public static Object readObject(Object tuple, ReferenceFieldActor referenceFieldActor) {
        try {
            final Field field = referenceFieldActor.toJava();
            field.setAccessible(true);
            return HostObjectAccess.hostToTarget(field.get(tuple));
        } catch (IllegalAccessException illegalAccessException) {
            accessError(referenceFieldActor);
            return null;
        }
    }

    /**
     * Write a byte into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param byteFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeByte(Object tuple, ByteFieldActor byteFieldActor, byte value) {
        try {
            final Field field = byteFieldActor.toJava();
            field.setAccessible(true);
            field.setByte(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(byteFieldActor);
        }
    }

    /**
     * Write a boolean into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param byteFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeBoolean(Object tuple, BooleanFieldActor booleanFieldActor, boolean value) {
        try {
            final Field field = booleanFieldActor.toJava();
            field.setAccessible(true);
            field.setBoolean(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(booleanFieldActor);
        }
    }

    /**
     * Write a short into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param shortFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeShort(Object tuple, ShortFieldActor shortFieldActor, short value) {
        try {
            final Field field = shortFieldActor.toJava();
            field.setAccessible(true);
            field.setShort(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(shortFieldActor);
        }
    }

    /**
     * Write a char into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param charFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeChar(Object tuple, CharFieldActor charFieldActor, char value) {
        try {
            final Field field = charFieldActor.toJava();
            field.setAccessible(true);
            field.setChar(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(charFieldActor);
        }
    }

    /**
     * Write an int into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param intFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeInt(Object tuple, IntFieldActor intFieldActor, int value) {
        try {
            final Field field = intFieldActor.toJava();
            field.setAccessible(true);
            field.setInt(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(intFieldActor);
        }
    }

    /**
     * Write a float into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param floatFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeFloat(Object tuple, FloatFieldActor floatFieldActor, float value) {
        try {
            final Field field = floatFieldActor.toJava();
            field.setAccessible(true);
            field.setFloat(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(floatFieldActor);
        }
    }

    /**
     * Write a long into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param longFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeLong(Object tuple, LongFieldActor longFieldActor, long value) {
        try {
            final Field field = longFieldActor.toJava();
            field.setAccessible(true);
            field.setLong(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(longFieldActor);
        }
    }

    /**
     * Write a double into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param doubleFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeDouble(Object tuple, DoubleFieldActor doubleFieldActor, double value) {
        try {
            final Field field = doubleFieldActor.toJava();
            field.setAccessible(true);
            field.setDouble(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(doubleFieldActor);
        }
    }

    /**
     * Write a word into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param wordFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeWord(Object tuple, WordFieldActor wordFieldActor, Word value) {
        try {
            final Field field = wordFieldActor.toJava();
            field.setAccessible(true);
            final Object object = UnsafeLoophole.wordToObject(value);
            field.set(tuple, object);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(wordFieldActor);
        }
    }

    /**
     * Write a reference into the specified field of the specified object.
     * @param tuple the object to which to write
     * @param referenceFieldActor the field actor representing the field to write
     * @param value the value to write into the field
     */
    public static void writeObject(Object tuple, ReferenceFieldActor referenceFieldActor, Object value) {
        try {
            final Field field = referenceFieldActor.toJava();
            field.setAccessible(true);
            field.set(tuple, value);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(referenceFieldActor);
        }
    }

}
