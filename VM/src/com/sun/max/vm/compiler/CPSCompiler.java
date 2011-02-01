/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler;

import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.runtime.*;

/**
 * The interface implemented by the legacy CPS compiler.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public interface CPSCompiler extends RuntimeCompiler {

    public static class Static {

        /**
         * Gets the package providing the platform specific CPS compiler.
         */
        @HOSTED_ONLY
        public static BootImagePackage defaultCPSCompilerPackage() {
            switch (platform().isa) {
                case AMD64:
                    return BootImagePackage.fromName("com.sun.max.vm.cps.b.c.d.e.amd64.target");
                default:
                    throw FatalError.unexpected(platform().isa.toString());
            }
        }

        /**
         * Gets the package providing the platform specific CPS compiler.
         */
        @HOSTED_ONLY
        public static String defaultCPSCompilerClassName() {
            switch (platform().isa) {
                case AMD64:
                    return "com.sun.max.vm.cps.b.c.d.e.amd64.target.AMD64CPSCompiler";
                default:
                    throw FatalError.unexpected(platform().isa.toString());
            }
        }

        private static CPSCompiler compiler;
        @HOSTED_ONLY
        private static BootImagePackage compilerPackage;

        @HOSTED_ONLY
        public static void setCompiler(CPSCompiler c) {
            assert compiler == null || compiler.getClass() == c.getClass();
            compiler = c;
            compilerPackage = BootImagePackage.fromClass(compiler.getClass());
        }

        @FOLD
        public static CPSCompiler compiler() {
            return compiler;
        }

        @HOSTED_ONLY
        public static boolean isCompiler(VMConfiguration vmConfiguration) {
            String value = CompilationScheme.optimizingCompilerOption.getValue();
            if (value.startsWith("com.sun.max.vm.cps")) {
                return true;
            }
            value = CompilationScheme.baselineCompilerOption.getValue();
            if (value != null && value.startsWith("com.sun.max.vm.cps")) {
                return true;
            }
            return false;
        }

        @HOSTED_ONLY
        public static boolean isCompilerPackage(VMConfiguration vmConfiguration, BootImagePackage cpsPackage) {
            return isCompiler(vmConfiguration);
        }

        /**
         * Do initialization specific to the installed BootstrapCompilerScheme compiler (if any).
         */
        @HOSTED_ONLY
        public static void initialize(PackageLoader packageLoader) {
            if (compiler != null) {
                compiler.createBuiltins(packageLoader);
                Builtin.register(compiler);
                compiler.createSnippets(packageLoader);
                Snippet.register();
            }
        }
    }

    /**
     * Starts up the compiler by building built-in operations.
     *
     * @param packageLoader a package loader which can be used to load classes that define built-ins
     */
    @HOSTED_ONLY
    void createBuiltins(PackageLoader packageLoader);

    /**
     * Starts up the compiler by building and optimizing snippets (i.e. internal pieces of IR
     * needed to translate from bytecodes to the compilers internal IR).
     *
     * @param packageLoader a package loader which can be used to load classes that define snippets
     */
    @HOSTED_ONLY
    void createSnippets(PackageLoader packageLoader);

    /**
     * Optimize the internal snippets.
     */
    @HOSTED_ONLY
    void compileSnippets();

    /**
     * Checks whether the compiler has finished compiling its internal snippets.
     *
     * @return {@code true} if the snippets are compiled; {@code false} otherwise
     */
    @HOSTED_ONLY
    boolean areSnippetsCompiled();

    /**
     * Checks whether this compiler implements the specified built-in directly, or whether
     * a native method is required to implement the operation.
     *
     * @param builtin the builtin to check
     * @return {@code true} if this compiler supports the specified built-in; {@code false} otherwise
     */
    boolean isBuiltinImplemented(Builtin builtin);
}
