/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm;

import java.io.*;
import java.util.*;

/**
 * This is a utility class for collecting the system properties, class path,
 * VM options, and arguments to a java command.
 */
public class JavaCommand {
    private final String[] mainArgs;
    private final List<String> vmOptions = new LinkedList<String>();
    private final List<String> sysProps = new LinkedList<String>();
    private final List<String> classPaths = new LinkedList<String>();
    private final List<String> arguments = new LinkedList<String>();

    /**
     * Create a java command with the specified main class.
     * @param mainClass
     */
    public JavaCommand(Class mainClass) {
        if (mainClass == null) {
            this.mainArgs = new String[0];
        } else {
            this.mainArgs = new String[] {mainClass.getName()};
        }
    }

    /**
     * Create a java command with the specified main class.
     * @param mainClassName
     */
    public JavaCommand(String mainClassName) {
        if (mainClassName == null) {
            this.mainArgs = new String[0];
        } else {
            this.mainArgs = new String[] {mainClassName};
        }
    }

    private JavaCommand(String[] mainArgs, List<String> vmOptions, List<String> sysProps, List<String> classPaths, List<String> arguments) {
        this.mainArgs = mainArgs.clone();
        this.vmOptions.addAll(vmOptions);
        this.sysProps.addAll(sysProps);
        this.classPaths.addAll(classPaths);
        this.arguments.addAll(arguments);
    }

    /**
     * Create a java command with the specified jar file.
     * @param jarFile the jar file which contains the command
     */
    public JavaCommand(File jarFile) {
        this.mainArgs = new String[] {"-jar", jarFile.getPath()};
    }

    /**
     * Add a system property to this java command.
     * @param name the name of the property
     * @param value the value of the property
     */
    public void addSystemProperty(String name, String value) {
        if (value != null) {
            sysProps.add("-D" + name + "=" + value);
        } else {
            sysProps.add("-D" + name);
        }
    }

    /**
     * Add an argument to this java command. This argument appears after the main class.
     * @param arg the argument to add to the main class
     */
    public void addArgument(String arg) {
        arguments.add(arg);
    }

    /**
     * Add the arguments to this java command. The arguments appear after the main class.
     * @param arg the argument to add to the main class
     */
    public void addArguments(String[] arg) {
        if (arg != null) {
            arguments.addAll(Arrays.asList(arg));
        }
    }

    /**
     * Add an option to the VM, which appears before the main class or java file.
     * @param option the option to add to the java command
     */
    public void addVMOption(String option) {
        vmOptions.add(option);
    }

    /**
     * Add options to the VM, which appear before the main class or java file.
     * @param option the options to add to the java command
     */
    public void addVMOptions(String[] option) {
        if (option != null) {
            vmOptions.addAll(Arrays.asList(option));
        }
    }

    /**
     * Add a classpath entry to this java command.
     * @param classpath the classpath entry that will be added
     */
    public void addClasspath(String classpath) {
        classPaths.add(classpath);
    }

    /**
     * Builds an array of strings by arranging options in the appropriate order
     * with the specified executable name at the head.
     * @param exec the executable which, if nonnull, will be prepended to the array of arguments
     * @return an array of strings in the correct format for the "java" command
     */
    public String[] getExecArgs(String exec) {
        final List<String> list = new ArrayList<String>();
        if (exec != null) {
            list.add(exec);
        }
        list.addAll(vmOptions);
        list.addAll(sysProps);
        if (!classPaths.isEmpty()) {
            list.add("-classpath");
            final StringBuilder builder = new StringBuilder();
            for (String path : classPaths) {
                if (builder.length() > 0) {
                    builder.append(':');
                }
                builder.append(path);
            }
            list.add(builder.toString());
        }
        list.addAll(Arrays.asList(mainArgs));
        list.addAll(arguments);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Returns a new copy of this JavaCommand object with identical options. Further updates to
     * this object will not affect the copy, and vice versa.
     * @return a new copy of this JavaCommand
     */
    public JavaCommand copy() {
        return new JavaCommand(mainArgs, vmOptions, sysProps, classPaths, arguments);
    }
}
