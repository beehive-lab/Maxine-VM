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
import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.Classpath.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.grip.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;

/**
 * Maps a boot image into memory and opens an inspection window,
 * in which one can inspect object and memory contents from the boot image.
 *
 * @author Bernd Mathiske
 */
public final class MaxineInspector {

    private static final String _TELE_LIBRARY_NAME = "tele";

    private static final OptionSet _options = new OptionSet();

    private static final Option<Boolean> _debug = _options.newBooleanOption("d", false,
        "Makes the inspector create a Maxine VM process as the target of inspection. If omitted or 'false', then the boot image is inspected.");
    private static final Option<File> _bootImage = _options.newFileOption("i", BinaryImageGenerator.getDefaultBootImageFilePath(),
        "Path to boot image file.");
    private static final Option<File> _bootJar = _options.newFileOption("j", BinaryImageGenerator.getDefaultBootImageJarFilePath(),
        "Boot jar file path.");
    private static final Option<List<String>> _classpath = _options.newStringListOption("cp", null, File.pathSeparatorChar,
        "Additional locations to use when searching for Java class files. These locations are searched after the jar file containing the " +
        "boot image classes but before the locations corresponding to the class path of this JVM process.");
    private static final Option<List<String>> _sourcepath = _options.newStringListOption("sourcepath", null, File.pathSeparatorChar,
        "Additional locations to use when searching for Java source files. These locations are searched before the default locations.");
    private static final Option<Boolean> _suspendBeforeRelocating = _options.newBooleanOption("b", false,
        "Forces the inspector to present the image before it has been relocated.");
    private static final Option<String> _vmArguments = _options.newStringOption("a", "",
        "Specifies the arguments to the target VM.");
    private static final Option<File> _commandFile = _options.newFileOption("c", "",
        "Executes the commands in a file on startup.");
    private static final Option<Integer> _debuggeeId = _options.newIntegerOption("id", -1,
        "Id of debuggee to connect to.");
    private static final Option<String> _logLevel = _options.newStringOption("logLevel", Level.SEVERE.getName(),
        "Level to set for java.util.logging root logger.");

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

    /*
    private static final Option<Integer> _processID = _options.newIntegerOption("p", 0,
            "when present, this option specifies that the inspector should attach to a running MaxineVM instance with " +
            "the given process ID. (unimplemented)");
     */

    private MaxineInspector() {
    }

    public static boolean suspendingBeforeRelocating() {
        return _suspendBeforeRelocating.getValue();
    }

    public static boolean debugging() {
        return _debug.getValue();
    }

    private static void checkClasspath(Classpath classpath) {
        for (Entry classpathEntry : classpath.entries()) {
            if (classpathEntry.isPlainFile()) {
                ProgramWarning.message("Class path entry is neither a directory nor a JAR file: " + classpathEntry);
            }
        }
    }

    private static void createAndShowGUI() {
        TeleVM teleVM = null;
        try {
            Trace.begin(1, "starting Inspector");

            // Configure the prototype class loader gets the class files used to build the image
            Classpath classpathPrefix = Classpath.EMPTY;
            final List<String> classpathList = _classpath.getValue();
            if (classpathList != null) {
                final Classpath extraClasspath = new Classpath(classpathList.toArray(new String[classpathList.size()]));
                classpathPrefix = classpathPrefix.prepend(extraClasspath);
            }
            classpathPrefix = classpathPrefix.prepend(_bootJar.getValue().getAbsolutePath());
            checkClasspath(classpathPrefix);
            final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
            PrototypeClassLoader.setClasspath(classpath);

            Prototype.loadLibrary(_TELE_LIBRARY_NAME);
            final File bootImageFile = _bootImage.getValue();

            Classpath sourcepath = JavaProject.getSourcePath(true);
            final List<String> sourcepathList = _sourcepath.getValue();
            if (sourcepathList != null) {
                sourcepath = sourcepath.prepend(new Classpath(sourcepathList.toArray(new String[sourcepathList.size()])));
            }
            checkClasspath(sourcepath);

            final String value = _vmArguments.getValue();
            final String[] commandLineArguments = "".equals(value) ? new String[0] : value.split(" ");

            if (debugging()) {
                teleVM = TeleVM.createNewChild(bootImageFile, sourcepath, commandLineArguments, _debuggeeId.getValue());
            } else {
                teleVM = TeleVM.createWithNoProcess(bootImageFile, sourcepath, !suspendingBeforeRelocating());
            }

            TeleDisassembler.initialize(teleVM);

            final TeleGripScheme teleGripScheme = (TeleGripScheme) teleVM.maxineVM().configuration().gripScheme();
            teleGripScheme.setTeleVM(teleVM);

            _inspection = new Inspection(teleVM, new StandardInspectorStyle(), new BasicInspectorGeometry());

            if (debugging() && !suspendingBeforeRelocating()) {
                teleVM.advanceToJavaEntryPoint();
            }

            synchronized (InspectorInterpreter._inspectionLock) {
                InspectorInterpreter._inspectionLock.notify();
            }

            _inspection.initialize();

            InspectorFrame.TitleBarListener.initialize();

            if (debugging()) {
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

                    final File commandFile = _commandFile.getValue();
                    if (commandFile != null && !commandFile.equals("")) {
                        FileCommands.executeCommandsFromFile(_inspection, commandFile.getPath());
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
            } else { // !debugging()
                // Initialize the CodeManager and ClassRegistry, which seems to keep some heap reads
                // in the BootImageInspecor from crashing when there's no VM running (mlvdv)
                if (!suspendingBeforeRelocating()) {
                    teleVM.teleCodeRegistry();
                }
                MethodInspector.Manager.make(_inspection);
                ObjectInspector.Manager.make(_inspection);
                _inspection.refreshAll(false);
            }
            Trace.end(1, "Inspector ready");


            _inspection.setVisible(true);

            return;
        } catch (Throwable throwable) {
            if (teleVM != null && teleVM.teleProcess() != null) {
                try {
                    teleVM.teleProcess().controller().terminate();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
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

    public static void main(String[] argv) {
        HostObjectAccess.setMainThread(Thread.currentThread());
        Trace.addTo(_options);
        _options.parseArguments(argv);
        final String logLevel = _logLevel.getValue();
        try {
            LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
        } catch (IllegalArgumentException e) {
            ProgramWarning.message("Invalid level specified for java.util.logging root logger: " + logLevel + " [using " + Level.SEVERE + "]");
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
        }
        initializeSwing();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    public static TeleVM test(String[] argv) {
        HostObjectAccess.setMainThread(Thread.currentThread());
        Trace.addTo(_options);
        _options.parseArguments(argv);
        initializeSwing();
        createAndShowGUI();
        return _inspection.teleVM();
    }

    public static VMConfiguration vmConfiguration() {
        return MaxineVM.target().configuration();
    }

}
