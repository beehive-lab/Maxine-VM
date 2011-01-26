/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
        final File workspaceDirectory = JavaProject.findWorkspaceDirectory();
        final String vmArguments =
            "-verbose:class " +
            "-classpath " +
            workspaceDirectory.toString() + "Tele/bin " +
            "test.com.sun.max.tele.HelloWorld";

        options.sourcepathOption.setValue(Arrays.asList(JavaProject.getSourcePath(TestTeleVM.class, true).toStringArray()));
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
