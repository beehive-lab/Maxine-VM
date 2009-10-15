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
package jtt.gc;

import com.sun.max.annotate.*;

/**
 * Tests that object parameters passed on the stack are not trashed by a GC.
 * That is, they are either protected by a GC refmap or are never live when
 * a GC can occur.
 *
 * @Harness: java
 * @Runs: (1) = true
 */
public class ObjectStackParams01 {
    // JIT Method
    public static boolean test(int ignore) {
        String obj = new String("Hello World");
        Object result = objStackParams(16, null, null, null, null, null, obj);
        return result.equals(obj);
    }

    /**
     * Method that will be passed some object parameters via the stack.
     */
    @UNSAFE
    private static Object objStackParams(int depth, Object a, Object b, Object c, Object
                    d, Object e, Object f) {
        if (depth > 0) {
            System.gc();
            return objStackParams(depth - 1, a, b, c, d, e, f);
        }
        return f;
    }
}
