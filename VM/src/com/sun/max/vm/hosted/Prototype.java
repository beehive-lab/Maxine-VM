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
package com.sun.max.vm.hosted;

import java.io.*;

import com.sun.max.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
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
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Prototype {

    /**
     * The default place where native libraries are placed by the make system.
     */
    private static final String LIBRARY_BUILD_PATH = "Native/generated/" + OperatingSystem.current().asPackageName() + "/";

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
            final File projectPath = JavaProject.findVcsProjectDirectory();
            final File workspacePath = projectPath.getParentFile();
            final String[] usrPaths = (String[]) WithoutAccessCheck.getStaticField(ClassLoader.class, "usr_paths");
            final String libraryPath = new File(workspacePath, LIBRARY_BUILD_PATH).getPath() + File.pathSeparator + Utils.toString(usrPaths, File.pathSeparator);
            JDKInterceptor.setLibraryPath(libraryPath);
            isPathHacked = true;
        }
        System.loadLibrary(name);
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
