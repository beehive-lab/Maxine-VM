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
package com.sun.max.vm.prototype;

import java.io.*;

import com.sun.max.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * This class simplifies loading native libraries in a {@linkplain MaxineVM#isHosted() hosted} environment.
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
     * The name of the default prototype native library.
     */
    public static final String PROTOTYPE_LIBRARY_NAME = "prototype";

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

    private static boolean isPrototypeLoaded;

    /**
     * Loads the native library containing code to be run in a {@linkplain MaxineVM#isHosted() hosted} environment.
     */
    public static void loadPrototypeLibrary() {
        if (!isPrototypeLoaded) {
            loadLibrary(PROTOTYPE_LIBRARY_NAME);
            isPrototypeLoaded = true;
        }
    }
}
