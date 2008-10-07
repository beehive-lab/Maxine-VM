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
/*VCSID=afc43203-d66b-4e8c-bf5b-023be7853c9d*/
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;

/**
 * Implements substitutions for {@link java.lang.Compiler java.lang.Compiler}.
 * This implementation simply provides substitutions that ignore all requests
 * to compile classes or enable/disable the compiler.
 *
 * @author Ben L. Titzer
 */
@METHOD_SUBSTITUTIONS(Compiler.class)
final class JDK_java_lang_Compiler {
    private JDK_java_lang_Compiler() {
    }

    /**
     * Register any native methods. There are none in this implementation.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Initialize the compiler.
     */
    @SUBSTITUTE
    private static void initialize() {
    }

    /**
     * Requests a class to be compiled. Ignored.
     * @see java.lang.Compiler#compileClass(Class)
     * @param clazz the class to compile
     * @return true if the class was compiled; false otherwise
     */
    @SUBSTITUTE
    public static boolean compileClass(Class clazz) {
        return false;
    }

    /**
     * Compiles all classes whose name matches the specified string. Ignored.
     * @see java.lang.Compiler#compileClasses(String)
     * @param string the pattern to match
     * @return true if the compilation succeeded; false otherwise
     */
    @SUBSTITUTE
    public static boolean compileClasses(String string) {
        return false;
    }

    /**
     * Perform a compiler command. Ignored.
     * @param any an argument to the compiler command
     * @return some result
     */
    @SUBSTITUTE
    public static Object command(Object any) {
        return null;
    }

    /**
     * Enable the compiler. Ignored.
     */
    @SUBSTITUTE
    public static void enable() {
    }

    /**
     * Disable the compiler. Ignored.
     */
    @SUBSTITUTE
    public static void disable() {
    }
}
