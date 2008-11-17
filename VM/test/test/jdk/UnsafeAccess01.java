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
package test.jdk;

import java.lang.reflect.*;

import sun.misc.*;

/*
 * @Harness: java
 * @Runs: 0=42
 */
public class UnsafeAccess01 {

    private int _field = 42;

    public static int test(int arg) throws SecurityException, NoSuchFieldException, IllegalAccessException {
        final Unsafe unsafe = getUnsafe();

        final UnsafeAccess01 object = new UnsafeAccess01();
        final Field field = UnsafeAccess01.class.getDeclaredField("_field");
        final long offset = unsafe.objectFieldOffset(field);
        final int value = unsafe.getInt(object, offset);
        return value;
    }

    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }
}
