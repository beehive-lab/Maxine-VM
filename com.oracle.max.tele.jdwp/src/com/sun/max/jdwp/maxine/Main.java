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
package com.sun.max.jdwp.maxine;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.max.ide.JavaProject;
import com.sun.max.jdwp.handlers.ArrayReferenceHandlers;
import com.sun.max.jdwp.handlers.ArrayTypeHandlers;
import com.sun.max.jdwp.handlers.ClassLoaderReferenceHandlers;
import com.sun.max.jdwp.handlers.ClassObjectReferenceHandlers;
import com.sun.max.jdwp.handlers.ClassTypeHandlers;
import com.sun.max.jdwp.handlers.EventRequestHandlers;
import com.sun.max.jdwp.handlers.JDWPSession;
import com.sun.max.jdwp.handlers.MethodHandlers;
import com.sun.max.jdwp.handlers.ObjectReferenceHandlers;
import com.sun.max.jdwp.handlers.ReferenceTypeHandlers;
import com.sun.max.jdwp.handlers.StackFrameHandlers;
import com.sun.max.jdwp.handlers.StringReferenceHandlers;
import com.sun.max.jdwp.handlers.ThreadGroupReferenceHandlers;
import com.sun.max.jdwp.handlers.ThreadReferenceHandlers;
import com.sun.max.jdwp.handlers.VirtualMachineHandlers;
import com.sun.max.jdwp.server.JDWPServer;
import com.sun.max.jdwp.vm.proxy.VMAccess;
import com.sun.max.program.Classpath;
import com.sun.max.program.Trace;
import com.sun.max.program.Classpath.Entry;
import com.sun.max.program.option.Option;
import com.sun.max.program.option.OptionSet;
import com.sun.max.tele.TeleVM;
import com.sun.max.tele.TeleVM.Options;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.tele.Inspectable;

/**
 * Class containing the main function to startup the Maxine JDWP server. The server is listening for incoming
 * connections on port.
 */
public class Main {

    private static final int PORT_RANGE_LENGTH = 50;

    private static final OptionSet options = new OptionSet();

    private static final Option<Integer> portOption = options.newIntegerOption("port", 2000,
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
        classpathPrefix = classpathPrefix.prepend(BootImageGenerator.getBootImageJarFile(null).getAbsolutePath());
        checkClasspath(classpathPrefix);

        final Classpath classpath = Classpath.fromSystem().prepend(classpathPrefix);
        HostedBootClassLoader.setClasspath(classpath);

        Prototype.loadLibrary(TeleVM.TELE_LIBRARY_NAME);

        final Classpath sourcepath = JavaProject.getSourcePath(Main.class, true);
        checkClasspath(sourcepath);

        TeleVM t = null;
        try {
            final Options options = new Options();
            options.sourcepathOption.setValue(Arrays.asList(sourcepath.toStringArray()));
            options.vmArguments.setValue(com.sun.max.Utils.toString(arguments, " "));
            t = TeleVM.create(options);
        } catch (BootImageException e) {
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
        final TeleVM vm = initTeleVM(new ArrayList<String>(), arguments);
        if (vm == null) {
            LOGGER.severe("Error creating TeleVM => exiting");
            return;
        }
        vm.lock();
        try {
            vm.modifyInspectableFlags(Inspectable.INSPECTED, true);
        } finally {
            vm.unlock();
        }

        // Execute until entry point is reached.
        LOGGER.info("Starting TeleVM");
        try {
            vm.advanceToJavaEntryPoint();
        } catch (IOException e) {
            LOGGER.severe("Exception occurred while starting TeleVM: " + e.toString());
            System.exit(-1);
        }
        LOGGER.info("Start point reached");

        final VMAccess vmAccess = vm.vmAccess();

        // For increasing the speed of the call later on.
        vmAccess.getAllReferenceTypes();
        vmAccess.getAllReferenceTypes();

        LOGGER.info("Creating JDWP server");
        final JDWPServer server = new JDWPServer();

        final JDWPSession session = new JDWPSession(vmAccess);

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

            final Integer firstPort = portOption.getValue();
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
