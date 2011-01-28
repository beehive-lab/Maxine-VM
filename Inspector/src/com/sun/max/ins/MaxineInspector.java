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
package com.sun.max.ins;

import javax.swing.*;

import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.Options;
import com.sun.max.vm.hosted.*;

/**
 * Interactive, visual tool for debugging a running instance of the Maxine VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class MaxineInspector {

    public static final String NAME = "Maxine Inspector";
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;
    public static final String VERSION_STRING = Integer.toString(MAJOR_VERSION) + "." + Integer.toString(MINOR_VERSION);
    public static final String HOME_URL = "http://labs.oracle.com/projects/maxine/";
    private static final int TRACE_VALUE = 1;

    private static final String tracePrefix = "[Inspector] ";

    private MaxineInspector() {
    }

    public static void main(final String[] args) {
        Trace.begin(TRACE_VALUE, tracePrefix + "Initializing");
        final long startTimeMillis = System.currentTimeMillis();

        final Options options = new Options();
        Trace.addTo(options);
        final Option<Boolean> helpOption = options.newBooleanOption("help", false, "Show help message and exits.");
        options.parseArguments(args);

        if (helpOption.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }
        try {
            final MaxVM maxVM = TeleVM.create(options);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    Inspection.initializeSwing();
                    final Inspection inspection = new Inspection(maxVM, options);
                    if (maxVM.inspectionMode() == MaxInspectionMode.IMAGE) {
                        // Bring up the boot image info inspector as a starting point for browsing
                        BootImageInspector.make(inspection).highlight();
                    }
                }
            });
        } catch (BootImageException bootImageException) {
            InspectorError.unexpected("could not load boot image", bootImageException);
        } catch (Exception exception) {
            InspectorError.unexpected(tracePrefix + "failed: ", exception);
        }
        Trace.end(TRACE_VALUE, tracePrefix + "Initializing", startTimeMillis);
    }

    public static String description() {
        return "The " + NAME + " Ver. " + VERSION_STRING + "  <" + HOME_URL + ">";
    }


}
