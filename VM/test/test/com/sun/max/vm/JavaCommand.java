/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package test.com.sun.max.vm;

import java.io.*;
import java.util.*;

/**
 * This is a utility class for collection the system properties, class path,
 * VM options, and arguments to a java command.
 *
 * @author Ben L. Titzer
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
     * @param jarFile
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
            for (String s : arg) {
                arguments.add(s);
            }
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
            for (String s : option) {
                vmOptions.add(s);
            }
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
    public String[] getExecutableCommand(String exec) {
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
        for (String arg : mainArgs) {
            list.add(arg);
        }
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
