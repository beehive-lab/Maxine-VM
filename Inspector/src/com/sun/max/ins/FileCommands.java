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

import java.io.*;

import com.sun.max.tele.object.*;
import com.sun.max.vm.type.*;

/**
 * TODO: The break command uses code that was mostly cut and paste from
 * TargetMethodSearchDialog. The latter should be refactored to disentangle
 * the logic used here from the GUI aspects.
 * TODO: The command parsing is very picky - it needs a real lexer to deal with white space properly.
 *
 * @author Mick Jordan
 */

public class FileCommands {
    private static final String DEFAULT_COMMAND_FILE_PROPERTY = "max.ins.defaultcommandfile";
    private static final String USER_HOME_PROPERTY = "user.home";
    private static final String DEFAULT_COMMAND_FILE = ".max_ins_commands";
    private static final String ACTION_NAME = "Execute Commands from File...";

    private static String _defaultCommandFile;
    private static int _lineNumber;

    static {
        _defaultCommandFile = System.getProperty(DEFAULT_COMMAND_FILE_PROPERTY);
        if (_defaultCommandFile == null) {
            final String userHome = System.getProperty(USER_HOME_PROPERTY);
            if (userHome != null) {
                _defaultCommandFile = userHome + File.separator + DEFAULT_COMMAND_FILE;
            }
        }

    }

    public static String actionName() {
        return ACTION_NAME;
    }

    public static String defaultCommandFile() {
        return _defaultCommandFile;
    }

    public static void executeCommandsFromFile(Inspection inspection, String filename) {
        _lineNumber = 0;
        BufferedReader bs = null;
        try {
            bs = new BufferedReader(new FileReader(filename));
            while (true) {
                final String line = bs.readLine();
                if (line == null) {
                    break;
                }
                _lineNumber++;
                if (line.length() == 0) {
                    continue;
                }
                try {
                    doCommand(inspection, line);
                } catch (CommandException ex) {
                    inspection.errorMessage("Command failed: " + ex.getMessage(), ACTION_NAME);
                }
            }
        } catch (IOException ex) {
            inspection.errorMessage("Failed to open file: " + filename, ACTION_NAME);
        } finally {
            if (bs != null) {
                try {
                    bs.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private static void doCommand(Inspection inspection, String line) throws CommandException {
        final int index = line.indexOf(' ');
        final String command = (index < 0) ? line : line.substring(0, index);
        final String arguments = (index < 0) ? "" : line.substring(index + 1);
        if (command.equals("break")) {
            doBreak(inspection, arguments);
        }
    }

    private static void doBreak(Inspection inspection, String arg) throws CommandException {
        final int index = arg.lastIndexOf('.');
        if (index < 0) {
            throw new CommandException("syntax error: class name missing");
        }
        final String className = arg.substring(0, index);
        final String methodSignature = arg.substring(index + 1);
        final TeleClassActor teleClassActor = inspection.teleVM().teleClassRegistry().findTeleClassActorByType(JavaTypeDescriptor.getDescriptorForJavaString(className));
        if (teleClassActor == null) {
            throw new CommandException("failed to find class: " + className + " (not qualified or misspelled?)");
        }
        boolean found = false;
        for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
            if (teleClassMethodActor.classMethodActor().format("%n(%p)").equals(methodSignature)) {
                found = true;
                teleClassMethodActor.setTargetBreakpointAtEntry();
                break;
            }
        }
        if (!found) {
            throw new CommandException("failed to find method: " + arg);
        }
    }

    static class CommandException extends Exception {
        CommandException(String message) {
            super("error at line " + +_lineNumber + ": " + message);
        }
    }
}
