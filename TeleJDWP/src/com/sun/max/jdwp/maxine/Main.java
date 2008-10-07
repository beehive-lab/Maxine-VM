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
/*VCSID=d5891d70-637b-4330-8ab3-995a62aa59d8*/
package com.sun.max.jdwp.maxine;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.sun.max.ide.*;
import com.sun.max.jdwp.handlers.*;
import com.sun.max.jdwp.server.*;
import com.sun.max.program.*;
import com.sun.max.program.Classpath.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.grip.*;
import com.sun.max.vm.prototype.*;

/**
 * Class containing the main function to startup the Maxine JDWP server. The server is listening for incoming
 * connections on port {@link Main.PORT}.
 *
 * @author Thomas Wuerthinger
 */
public class Main {

    private static final int PORT_RANGE_LENGTH = 50;

    private static final OptionSet _options = new OptionSet();

    private static final Option<Integer> _portOption = _options.newIntegerOption("port", 2000,
                    "The port to listen on for client requests. If the socket cannot be opened" +
                    "then the next " + PORT_RANGE_LENGTH + " successive ports are tried.");

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Creates a TeleVM object by starting up a child process.
     *
     * @param classpathList the classpath used for the PrototypeClassLoader
     * @param arguments the program arguments for the child VM
     * @return a TeleVM object representing a child process
     */
    private static TeleVM initTeleVM(List<String> classpathList, String[] arguments) {

        Classpath classpathPrefix = Classpath.EMPTY;
        if (classpathList != null) {
            final Classpath extraClasspath = new Classpath(classpathList.toArray(new String[classpathList.size()]));
            classpathPrefix = classpathPrefix.prepend(extraClasspath);
        }
        classpathPrefix = classpathPrefix.prepend(BinaryImageGenerator.getDefaultBootImageJarFilePath().getAbsolutePath());
        checkClasspath(classpathPrefix);

        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        PrototypeClassLoader.setClasspath(classpath);

        Prototype.loadLibrary("inspector");

        final Classpath sourcepath = JavaProject.getSourcePath(true);
        checkClasspath(sourcepath);

        final File bootImageFile = BinaryImageGenerator.getDefaultBootImageFilePath();
        TeleVM t = null;
        try {
            t = TeleVM.createNewChild(bootImageFile, sourcepath, arguments, -1);
        } catch (BootImageException e) {
            LOGGER.severe("Exception occurred while creating TeleVM process: " + e.toString());
            System.exit(-1);
        } catch (IOException e) {
            LOGGER.severe("Exception occurred while creating TeleVM process: " + e.toString());
            System.exit(-1);
        }
        return t;
    }

    /**
     * Checks whether the entries in the classpath are valid. A warning message is printed for invalid entries.
     *
     * @param classpath the classpath object to be checked
     */
    private static void checkClasspath(Classpath classpath) {
        for (Entry classpathEntry : classpath.entries()) {
            if (classpathEntry.isPlainFile()) {
                LOGGER.warning("Class path entry is neither a directory nor a JAR file: " + classpathEntry);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Trace.on(1);

        // Initialize command line arguments.
        LOGGER.info("Parsing");
        final String[] defaultCommandlineArguments = new String[] {"-classpath", "/home/tw224376/maxine/MaxineVM/bin", "util.HelloWorld"};

        String[] arguments = args;
        if (args.length == 0) {
            LOGGER.info("No arguments found, using default arguments: " + defaultCommandlineArguments);
            arguments = defaultCommandlineArguments;
        }

        // Initialize TeleVM.
        LOGGER.info("Initializing TeleVM");
        final TeleVM teleVM = initTeleVM(new ArrayList<String>(), arguments);
        if (teleVM == null) {
            LOGGER.severe("Error creating TeleVM => exiting");
            return;
        }

        // Set Tele VM in grip scheme.
        final TeleGripScheme teleGripScheme = (TeleGripScheme) teleVM.maxineVM().configuration().gripScheme();
        teleGripScheme.setTeleVM(teleVM);

        // Execute until entry point is reached.
        LOGGER.info("Starting TeleVM");
        try {
            teleVM.advanceToJavaEntryPoint();
        } catch (IOException e) {
            LOGGER.severe("Exception occurred while starting TeleVM: " + e.toString());
            System.exit(-1);
        }
        LOGGER.info("Start point reached");

        // For increasing the speed of the call later on.
        teleVM.getAllReferenceTypes();
        teleVM.getAllReferenceTypes();

        LOGGER.info("Creating JDWP server");
        final JDWPServer server = new JDWPServer();

        final JDWPSession session = new JDWPSession(teleVM);

        new ArrayReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new ArrayTypeHandlers(session).registerWith(server.commandHandlerRegistry());
        new ClassLoaderReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new ClassObjectReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new ClassTypeHandlers(session).registerWith(server.commandHandlerRegistry());
        new EventRequestHandlers(session).registerWith(server.commandHandlerRegistry());
        new MethodHandlers(session).registerWith(server.commandHandlerRegistry());
        new ObjectReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new ReferenceTypeHandlers(session).registerWith(server.commandHandlerRegistry());
        new StackFrameHandlers(session).registerWith(server.commandHandlerRegistry());
        new StringReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new ThreadGroupReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new ThreadReferenceHandlers(session).registerWith(server.commandHandlerRegistry());
        new VirtualMachineHandlers(session).registerWith(server.commandHandlerRegistry());

        try {

            final Integer firstPort = _portOption.getValue();
            ServerSocket serverSocket = null;
            final int lastPort = firstPort + PORT_RANGE_LENGTH - 1;
            for (int port = firstPort; serverSocket == null && port <= lastPort; ++port) {
                try {
                    serverSocket = new ServerSocket(port);
                } catch (IOException ioException) {
                    LOGGER.info("Could not open socket on port " + port);
                }
            }

            if (serverSocket != null) {
                System.out.println("Listening for connections on port " + serverSocket.getLocalPort() + "...");
                server.start(serverSocket);
            } else {
                System.out.println("Could not open a server socket on any port between " + firstPort + " and " + lastPort);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

}
