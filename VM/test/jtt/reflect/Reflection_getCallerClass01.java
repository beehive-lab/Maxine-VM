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
package jtt.reflect;

import sun.reflect.*;

/**
 * @Harness: java
 * @Runs: 0 = "sun.reflect.Reflection"; 1 = "test.reflect.Reflection_getCallerClass01$Caller1"; 2 = "test.reflect.Reflection_getCallerClass01$Caller2"
 * 
 * @author Bernd Mathiske
 */
public final class Reflection_getCallerClass01 {
    private Reflection_getCallerClass01() {
    }

    public static final class Caller1 {
        private Caller1() {
        }

        static String caller1(int depth) {
            return Reflection.getCallerClass(depth).getName();
        }
    }

    public static final class Caller2 {
        private Caller2() {
        }

        static String caller2(int depth) {
            return Caller1.caller1(depth);
        }
    }


    public static String test(int depth) {
        return Caller2.caller2(depth);
    }
}
