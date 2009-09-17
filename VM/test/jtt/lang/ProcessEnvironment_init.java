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
/*
 * @Harness: java
 * @Runs: 7 = 7
 */
package jtt.lang;

import java.util.*;

public final class ProcessEnvironment_init {

    private ProcessEnvironment_init() {
    }

    private static HashMap<Object, Object> theEnvironment;
    private static Map<Object, Object> theUnmodifiableEnvironment;

    public static int test(int v) {

        byte[][] environ = environ();
        theEnvironment = new HashMap<Object, Object>(environ.length / 2 + 3);

        for (int i = environ.length - 1; i > 0; i -= 2) {
            theEnvironment.put(Variable.valueOf(environ[i - 1]), Value.valueOf(environ[i]));
        }

        theUnmodifiableEnvironment = Collections.unmodifiableMap(new StringEnvironment(theEnvironment));

        return v;
    }

    private static final class StringEnvironment extends HashMap<Object, Object> {

        public StringEnvironment(HashMap<Object, Object> theenvironment) {
        }
    }

    private static final class Variable {

        public static Object valueOf(byte[] bs) {
            return new Object();
        }
    }

    private static final class Value {

        public static Object valueOf(byte[] bs) {
            return new Object();
        }
    }

    private static byte[][] environ() {
        return new byte[3][3];
    }
}
