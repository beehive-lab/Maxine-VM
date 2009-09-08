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
package com.sun.max.ins;

import javax.swing.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.*;
import com.sun.max.vm.prototype.*;

/**
 * Interactive, visual tool for debugging a running instance of the Maxine VM.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class MaxineInspector {

    private static final int TRACE_VALUE = 1;

    private static final String tracePrefix = "[MaxineInspector] ";

    private MaxineInspector() {
    }

    public static void main(final String[] args) {
        Trace.begin(TRACE_VALUE, tracePrefix + "Initializing");
        final long startTimeMillis = System.currentTimeMillis();

        Inspection.initializeSwing();
        final Options options = new Options(false);
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
                    new Inspection(maxVM);
                }
            });
        } catch (BootImageException bootImageException) {
            ProgramError.unexpected("could not load boot image", bootImageException);
        } catch (Exception exception) {
            ProgramError.unexpected(tracePrefix + "failed: ", exception);
        }
        Trace.end(TRACE_VALUE, tracePrefix + "Initializing", startTimeMillis);
    }

}
