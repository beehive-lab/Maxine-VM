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

import com.sun.max.ins.gui.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.*;
import com.sun.max.vm.prototype.*;

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

    private static final String tracePrefix = "[MaxineInspector] ";

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
        Trace.begin(TRACE_VALUE, tracePrefix + "Initializing");
        final long startTimeMillis = System.currentTimeMillis();

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
                    new Inspection(maxVM);
                }
            });
        } catch (BootImageException bootImageException) {
            ProgramError.unexpected("could not load boot image", bootImageException);
        } catch (Exception exception) {
            ProgramError.unexpected(tracePrefix + "failed: " + exception);
        }
        Trace.end(TRACE_VALUE, tracePrefix + "Initializing", startTimeMillis);
    }

}
