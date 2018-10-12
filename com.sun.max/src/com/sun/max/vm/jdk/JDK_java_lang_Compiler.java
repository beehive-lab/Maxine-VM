/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import com.sun.max.annotate.*;

/**
 * Implements substitutions for {@link java.lang.Compiler java.lang.Compiler}.
 * This implementation simply provides substitutions that ignore all requests
 * to compile classes or enable/disable the compiler.
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
