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
import com.sun.max.tele.TeleVM.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;


/**
 * Creates a running {@link TeleVM}, suitable for testing.
 *
 * @author Michael Van De Vanter
 */
public class TestTeleVM {

    private static final String _TELE_LIBRARY_NAME = "tele";
    private static TeleVM _teleVM = null;

    public static TeleVM create() {
        final Options options = new Options();

        final File bootJar = BinaryImageGenerator.getDefaultBootImageJarFilePath();
        Classpath classpathPrefix = Classpath.EMPTY;
        // May want to add something later
        classpathPrefix = classpathPrefix.prepend(bootJar.getAbsolutePath());
        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        PrototypeClassLoader.setClasspath(classpath);
        Prototype.loadLibrary(_TELE_LIBRARY_NAME);
        final File projectDirectory = JavaProject.findVcsProjectDirectory();
        final String vmArguments =
            "-verbose:class " +
            "-classpath " +
            projectDirectory.toString() + "/bin " +
            "test.com.sun.max.tele.HelloWorld";

        options._debugOption.setValue(Boolean.TRUE);
        options._bootImageFileOption.setValue(BinaryImageGenerator.getDefaultBootImageFilePath());
        options._sourcepathOption.setValue(Arrays.asList(JavaProject.getSourcePath(true).toStringArray()));
        options._vmArguments.setValue(vmArguments);

        try {
            _teleVM = TeleVM.create(options);
            TeleDisassembler.initialize(_teleVM);
        } catch (BootImageException e) {
            System.out.println("Failed to load boot image " + BinaryImageGenerator.getDefaultBootImageFilePath().toString());
            e.printStackTrace();
        }

        return _teleVM;
    }

    public static void main(String[] argv) {
        HostObjectAccess.setMainThread(Thread.currentThread());
        LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        System.out.println("creating VM");
        final TeleVM teleVM = create();
        System.out.println("end creating VM");
        try {
            teleVM.controller().resume(true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
