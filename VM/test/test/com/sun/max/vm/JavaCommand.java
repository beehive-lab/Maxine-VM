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
    private final String[] _mainArgs;
    private final List<String> _vmOptions = new LinkedList<String>();
    private final List<String> _sysProps = new LinkedList<String>();
    private final List<String> _classPaths = new LinkedList<String>();
    private final List<String> _arguments = new LinkedList<String>();

    /**
     * Create a java command with the specified main class.
     * @param mainClass
     */
    public JavaCommand(Class mainClass) {
        if (mainClass == null) {
            _mainArgs = new String[0];
        } else {
            _mainArgs = new String[] {mainClass.getName()};
        }
    }

    /**
     * Create a java command with the specified main class.
     * @param mainClassName
     */
    public JavaCommand(String mainClassName) {
        if (mainClassName == null) {
            _mainArgs = new String[0];
        } else {
            _mainArgs = new String[] {mainClassName};
        }
    }

    /**
     * Create a java command with the specified jar file.
     * @param jarFile
     */
    public JavaCommand(File jarFile) {
        _mainArgs = new String[] {"-jar", jarFile.getPath()};
    }

    /**
     * Add a system property to this java command.
     * @param name the name of the property
     * @param value the value of the property
     */
    public void addSystemProperty(String name, String value) {
        _sysProps.add("-D" + name + "=" + value);
    }

    /**
     * Add an argument to this java command. This argument appears after the main class.
     * @param arg the argument to add to the main class
     */
    public void addArgument(String arg) {
        _arguments.add(arg);
    }

    /**
     * Add an option to the VM, which appears before the main class or java file.
     * @param option the option to add to the java command
     */
    public void addVMOption(String option) {
        _vmOptions.add(option);
    }

    /**
     * Add a classpath entry to this java command.
     * @param classpath the classpath entry that will be added
     */
    public void addClasspath(String classpath) {
        _classPaths.add(classpath);
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
        list.addAll(_vmOptions);
        list.addAll(_sysProps);
        if (!_classPaths.isEmpty()) {
            list.add("-classpath");
            final StringBuilder builder = new StringBuilder();
            for (String path : _classPaths) {
                if (builder.length() > 0) {
                    builder.append(':');
                }
                builder.append(path);
            }
            list.add(builder.toString());
        }
        for (String arg : _mainArgs) {
            list.add(arg);
        }
        list.addAll(_arguments);
        return list.toArray(new String[list.size()]);
    }
}
