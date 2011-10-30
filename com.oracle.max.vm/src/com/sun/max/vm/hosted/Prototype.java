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
package com.sun.max.vm.hosted;

import java.io.*;

import com.sun.max.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;

/**
 * Base class for VM prototypes.
 * A <i>prototype</i> is a representation of the entirety of the VM before bootstrap,
 * in some other format than the eventual fully bootstrapped executable.
 * The {@link PrototypeGenerator} progresses through several prototype stages:
 * <ol>
 * <li>{@link JavaPrototype} - all configured VM packages loaded, actors created for all constituents</li>
 * <li>{@link CompiledPrototype} - all <i>necessary</i> methods compiled (i.e. those methods without which the VM won't run)</li>
 * <li>{@link GraphPrototype} - a collection containing every object that will be part of the boot image</li>
 * </ol>
 *
 * An <i>image generator</i> (such as the {@link BootImageGenerator}) writes a graph prototype to a <i>boot image</i>
 * which is a binary representation of the VM, including all objects and all precompiled machine code.
 * <p>
 * This class also simplifies loading native libraries in a {@linkplain MaxineVM#isHosted() hosted} environment.
 */
public abstract class Prototype {

    /**
     * The root of the directory tree where generated files are placed by the build system.
     */
    static final String GENERATED_ROOT = "com.oracle.max.vm.native" + File.separator + "generated";

    /**
     * The root of the (target) OS specific directory tree for generated files.
     */
    static final String TARGET_GENERATED_ROOT = GENERATED_ROOT + File.separator + OS.fromName(System.getProperty(Platform.OS_PROPERTY, OS.current().name())).asPackageName();

    /**
     * The root on the host OS for generated files, specifically the {@link #HOSTED_LIBRARY_NAME hosted library}.
     */
    private static final String HOSTED_GENERATED_ROOT =  GENERATED_ROOT + File.separator + OS.current().asPackageName();

    /**
     * The name of the default hosted native library.
     */
    public static final String HOSTED_LIBRARY_NAME = "hosted";

    /**
     * A status variable indicating whether modifications to the underlying "java.library.path" have been made.
     */
    private static boolean isPathHacked;

    /**
     * Loads a native library in the prototype with the specified name. This method automatically finds the correct path
     * and adds into the JDK's internal path for looking up native libraries so that it is not necessary to specify it
     * as an environment variable when launching the java program.
     *
     * @param name the name of the library as a string, without any prefixes or suffixes such as "lib*", "*.so",
     *            "*.dll", etc
     */
    public static synchronized void loadLibrary(String name) {
        if (!isPathHacked) {
            final File workspacePath =  JavaProject.findWorkspaceDirectory();
            final String[] usrPaths = (String[]) WithoutAccessCheck.getStaticField(ClassLoader.class, "usr_paths");
            final String maxLibPath = name.equals(HOSTED_LIBRARY_NAME) ? HOSTED_GENERATED_ROOT : TARGET_GENERATED_ROOT;
            final String libraryPath = new File(workspacePath, maxLibPath).getPath() + File.pathSeparator + Utils.toString(usrPaths, File.pathSeparator);
            JDKInterceptor.setLibraryPath(libraryPath);
            isPathHacked = true;
        }
        try {
            System.loadLibrary(name);
        } catch (UnsatisfiedLinkError e) {
            String lib = System.mapLibraryName(name);
            ProgramWarning.message("Could not load native library: " + lib);
        }
    }

    private static boolean isHostedLoaded;

    /**
     * Loads the native library containing code to be run in a {@linkplain MaxineVM#isHosted() hosted} environment.
     */
    public static void loadHostedLibrary() {
        if (!isHostedLoaded) {
            loadLibrary(HOSTED_LIBRARY_NAME);
            isHostedLoaded = true;
        }
    }
}
