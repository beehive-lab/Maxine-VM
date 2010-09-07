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
package test.com.sun.max.tele;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.sun.max.ide.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.Options;
import com.sun.max.vm.hosted.*;

/**
 * Creates a running {@link TeleVM}, suitable for testing.
 *
 * @author Michael Van De Vanter
 */
public class TestTeleVM {

    private static final String TELE_LIBRARY_NAME = "tele";
    private static TeleVM teleVM = null;

    public static TeleVM create() {
        final Options options = new Options();

        final File bootJar = BootImageGenerator.getBootImageJarFile(null);
        Classpath classpathPrefix = Classpath.EMPTY;
        // May want to add something later
        classpathPrefix = classpathPrefix.prepend(bootJar.getAbsolutePath());
        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        HostedBootClassLoader.setClasspath(classpath);
        Prototype.loadLibrary(TELE_LIBRARY_NAME);
        final File projectDirectory = JavaProject.findVcsProjectDirectory();
        final String vmArguments =
            "-verbose:class " +
            "-classpath " +
            projectDirectory.toString() + "/bin " +
            "test.com.sun.max.tele.HelloWorld";

        options.sourcepathOption.setValue(Arrays.asList(JavaProject.getSourcePath(true).toStringArray()));
        options.vmArguments.setValue(vmArguments);

        try {
            teleVM = TeleVM.create(options);
        } catch (BootImageException e) {
            System.out.println("Failed to load boot image " + BootImageGenerator.getBootImageFile(null).toString());
            e.printStackTrace();
        }

        return teleVM;
    }

    public static void main(String[] argv) {
        //HostObjectAccess.setMainThread(Thread.currentThread());
        LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        System.out.println("creating VM");
        final TeleVM teleVM = create();
        System.out.println("end creating VM");
        try {
            teleVM.resume(true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
