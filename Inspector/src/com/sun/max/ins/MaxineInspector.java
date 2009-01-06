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
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.*;

/**
 * Maps a boot image into memory and opens an inspection window,
 * in which one can inspect object and memory contents from the boot image.
 *
 * @author Bernd Mathiske
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

    private static void createAndShowGUI(Options options) {
        try {
            Trace.begin(TRACE_VALUE, _tracePrefix + "Initializing");

            final TeleVM teleVM = TeleVM.create(options);
            TeleDisassembler.initialize(teleVM);

            _inspection = new Inspection(teleVM, new StandardInspectorStyle(), new BasicInspectorGeometry());

            _inspection.initialize();

            InspectorFrame.TitleBarListener.initialize();

            if (!teleVM.isReadOnly()) {
                try {
                    // Choose an arbitrary thread as the "current" thread. If the inspector is
                    // creating the process to be debugged (as opposed to attaching to it), then there
                    // should only be one thread.
                    final IterableWithLength<TeleNativeThread> threads = _inspection.teleProcess().threads();
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
                    VmThreadLocalsInspector.make(_inspection);
                    StackInspector.make(_inspection, _inspection.focus().thread());
                    BreakpointsInspector.make(_inspection);
                    MethodInspector.Manager.make(_inspection);
                    ObjectInspector.Manager.make(_inspection);
                    _inspection.focus().setCodeLocation(new TeleCodeLocation(teleVM, _inspection.focus().thread().instructionPointer()), false);
                    _inspection.refreshAll(false);
                } catch (Throwable throwable) {
                    System.err.println("Error during initialization");
                    throwable.printStackTrace();
                    System.exit(1);
                }
            } else {
                // Initialize the CodeManager and ClassRegistry, which seems to keep some heap reads
                // in the BootImageInspecor from crashing when there's no VM running (mlvdv)
                if (teleVM.isBootImageRelocated()) {
                    teleVM.teleCodeRegistry();
                }
                MethodInspector.Manager.make(_inspection);
                ObjectInspector.Manager.make(_inspection);
                _inspection.refreshAll(false);
            }
            Trace.end(TRACE_VALUE, _tracePrefix + "Initializing");


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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final Options options = new Options();
                Trace.addTo(options);
                options.parseArguments(args);
                createAndShowGUI(options);
            }
        });
    }

    public static VMConfiguration vmConfiguration() {
        return MaxineVM.target().configuration();
    }
}
