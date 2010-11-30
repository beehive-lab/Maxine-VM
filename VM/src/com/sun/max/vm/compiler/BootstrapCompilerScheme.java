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
package com.sun.max.vm.compiler;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * This compiler interface extends the {@link RuntimeCompilerScheme} to denote the extra operations
 * required for a compiler that can be used to boot strap the Maxine VM.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public interface BootstrapCompilerScheme extends RuntimeCompilerScheme {

    public static class Static {
        private static BootstrapCompilerScheme compiler;

        public static void setCompiler(BootstrapCompilerScheme comp) {
            assert compiler == null;
            compiler = comp;
        }

        public static BootstrapCompilerScheme compiler() {
            assert compiler != null;
            return compiler;
        }

        /**
         * Do initialization specific to the installed BootstrapCompilerScheme compiler (if any).
         */
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
