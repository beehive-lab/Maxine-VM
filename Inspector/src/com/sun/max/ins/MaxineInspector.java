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

import java.awt.event.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.*;
import com.sun.max.tele.debug.*;

/**
 * Interactive, visual tool for development of the Maxine VM.
 * <BR>
 * Depending on a command line option, either:
 * <UL>
 * <LI>Starts a running VM from a boot image, and opens an inspection window
 * providing remote access and debugging; or</LI>
 * <LI>Maps a boot image into memory and opens an inspection window
 * in which one can inspect object and memory contents from the boot image.</LI>
 * </UL>
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class MaxineInspector {

    private static final int TRACE_VALUE = 1;

    private static final String _tracePrefix = "[MaxineInspector] ";

    private static Inspection _inspection;

    public static int mouseButtonWithModifiers(MouseEvent mouseEvent) {
        if (OperatingSystem.current() == OperatingSystem.DARWIN && mouseEvent.getButton() == MouseEvent.BUTTON1) {
            if (mouseEvent.isControlDown()) {
                if (!mouseEvent.isAltDown()) {
                    return MouseEvent.BUTTON3;
                }
            } else if (mouseEvent.isAltDown()) {
                return MouseEvent.BUTTON2;
            }
        }
        return mouseEvent.getButton();
    }

    private MaxineInspector() {
    }

    private static void createAndShowGUI(MaxVM maxVM) {
        try {
            Trace.begin(TRACE_VALUE, _tracePrefix + "Initializing");
            final long startTimeMillis = System.currentTimeMillis();
            _inspection = new Inspection(maxVM);
            //TODO (mlvdv) don't know if the separate initialization is still necessary
            _inspection.initialize();
            InspectorFrame.TitleBarListener.initialize();

            if (!maxVM.isReadOnly()) {
                try {
                    // Choose an arbitrary thread as the "current" thread. If the inspector is
                    // creating the process to be debugged (as opposed to attaching to it), then there
                    // should only be one thread.
                    final IterableWithLength<TeleNativeThread> threads = _inspection.maxVM().threads();
                    TeleNativeThread nonJavaThread = null;
                    for (TeleNativeThread thread : threads) {
                        if (thread.isJava()) {
                            _inspection.focus().setThread(thread);
                            nonJavaThread = null;
                            break;
                        }
                        nonJavaThread = thread;
                    }
                    if (nonJavaThread != null) {
                        _inspection.focus().setThread(nonJavaThread);
                    }

                    ThreadsInspector.make(_inspection);
                    RegistersInspector.make(_inspection);
                    ThreadLocalsInspector.make(_inspection);
                    StackInspector.make(_inspection);
                    BreakpointsInspector.make(_inspection);
                    MethodInspector.Manager.make(_inspection);
                    ObjectInspectorFactory.make(_inspection);
                    _inspection.focus().setCodeLocation(maxVM.createCodeLocation(_inspection.focus().thread().instructionPointer()), false);
                    _inspection.refreshAll(false);
                } catch (Throwable throwable) {
                    System.err.println("Error during initialization");
                    throwable.printStackTrace();
                    System.exit(1);
                }
            } else {
                // Initialize the CodeManager and ClassRegistry, which seems to keep some heap reads
                // in the BootImageInspecor from crashing when there's no VM running (mlvdv)
//                if (teleVM.isBootImageRelocated()) {
//                    teleVM.teleCodeRegistry();
//                }
                MethodInspector.Manager.make(_inspection);
                ObjectInspectorFactory.make(_inspection);
                _inspection.refreshAll(false);
            }
            Trace.end(TRACE_VALUE, _tracePrefix + "Initializing", startTimeMillis);
            _inspection.setVisible(true);
            return;
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not load boot image", throwable);
        }
    }

    /**
     * Initializes the UI system to meet current requirements such as the requirement that the L&F for {@link InspectorFrame}s
     * renders a "frame icon" at the top left of a frame's title bar. Using the Metal L&F is the mechanism currently employed
     * for meeting this requirement.
     */
    public static void initializeSwing() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            ProgramError.unexpected("Could not set L&F to MetalLookAndFeel");
        }

        //System.setProperty("apple.laf.useScreenMenuBar", "true");
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
    }

    public static void main(final String[] args) {
        initializeSwing();
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
                    createAndShowGUI(maxVM);
                }
            });
        } catch (Exception exception) {
            System.out.println(_tracePrefix + "failed: " + exception);
        }
    }

}
