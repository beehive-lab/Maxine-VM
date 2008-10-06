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
/*VCSID=301fea75-ad97-4567-8e71-c7963a2dfcdd*/
package com.sun.max.ide;

import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;

/**
 * To pass command line arguments to a JUnit test, set the system property "max.test.arguments" on the VM command line or
 * where ever VM arguments are specified in the IDE. Multiple command line arguments must be separated with a space or
 * a colon. For example, to pass these arguments to a JUnit test:
 * <p><pre>
 *     -vmclasses
 *     -jcklist=test/test/com/sun/max/vm/verifier/jck.classes.txt
 * </pre><p>
 * requires defining the system property on the VM command line as follows:
 * <p><pre>
 *     -Dmax.test.arguments=-vmclasses -jcklist=test/test/com/sun/max/vm/verifier/jck.classes.txt
 * </pre><p>
 * or:
 * <p><pre>
 *     -Dmax.test.arguments=-vmclasses:-jcklist=test/test/com/sun/max/vm/verifier/jck.classes.txt
 * </pre><p>
 * If using the latter (colon-separated) form, then colons in an argument must be escaped with a backslash.
 */
public class MaxTestCase extends TestCase {

    public MaxTestCase() {
        this(null);
        setName(getClass().getName());
    }

    public MaxTestCase(String name) {
        super(name);
    }

    public static final String PROGRAM_ARGUMENTS_SYSTEM_PROPERTY = "max.test.arguments";

    private static String[] _programArguments;

    public static String[] getProgramArguments() {
        if (_programArguments == null) {
            String args = System.getProperty(PROGRAM_ARGUMENTS_SYSTEM_PROPERTY);
            if (args != null) {
                args = args.replace("\\:", "\u0000");
                _programArguments = args.split("[\\s:]+");
                for (int i = 0; i != _programArguments.length; ++i) {
                    _programArguments[i] = _programArguments[i].replace("\u0000", ":");
                }
            } else {
                _programArguments = new String[0];
            }
        }
        return _programArguments;
    }

    public static void setProgramArguments(String[] args) {
        _programArguments = args.clone();
    }

    /**
     * @see #createSuite(MaxPackage, boolean, boolean, Class...)
     */
    public static TestSuite createSuite(MaxPackage maxPackage, boolean addClasses, Class... lastTests) {
        return createSuite(maxPackage, addClasses, false, lastTests);
    }

    public static TestSuite createAutoTestSuite(MaxPackage maxPackage, boolean scanSubPackages, Class... lastTests) {
        final TestSuite suite = new TestSuite(maxPackage.name());
        final Set<Class<? extends TestCase>> testClasses = new LinkedHashSet<Class<? extends TestCase>>();
        addTests(maxPackage, testClasses, lastTests);
        if (scanSubPackages) {
            final Classpath systemClasspath = Classpath.fromSystem();
            for (MaxPackage subPackage : maxPackage.getTransitiveSubPackages(systemClasspath)) {
                try {
                    Class.forName(subPackage.name() +  ".AutoTest");
                } catch (ClassNotFoundException e) {
                    addTests(subPackage, testClasses, lastTests);
                }
            }
        }
        for (Class<? extends TestCase> testClass : testClasses) {
            suite.addTestSuite(testClass);
        }
        return suite;
    }

    /**
     * Creates a JUnit test suite for a given package and populates it with all the classes in that package that
     * subclass {@link TestCase} if {@code addClasses == true}.
     * 
     * 
     * @param maxPackage
     *            the package to create a test suite for
     * @param addClasses
     *            specifies if the package(s) are to be scanned for tests
     * @param scanSubPackages
     *            specifies if the sub-packages of {@code maxPackage} should be processed
     * @param lastTests
     *            adds these tests to the end of the suite. This is useful to run long running tests last.
     * @return
     */
    public static TestSuite createSuite(MaxPackage maxPackage, boolean addClasses, boolean scanSubPackages, Class... lastTests) {
        final TestSuite suite = new TestSuite(maxPackage.name());
        if (addClasses) {
            final Set<Class<? extends TestCase>> testClasses = new LinkedHashSet<Class<? extends TestCase>>();
            addTests(maxPackage, testClasses, lastTests);
            if (scanSubPackages) {
                for (MaxPackage subPackage : maxPackage.getTransitiveSubPackages(Classpath.fromSystem())) {
                    addTests(subPackage, testClasses, lastTests);
                }
            }
            for (Class<? extends TestCase> testClass : testClasses) {
                suite.addTestSuite(testClass);
            }
        }
        return suite;
    }

    /**
     * Adds all the classes in {@code javaPackage} that subclass {@link TestCase} to {@code suite}.
     * 
     * @param lastTests adds these tests to the end of the suite
     */
    public static void addTests(MaxPackage maxPackage, Set<Class<? extends TestCase>> testClasses, Class... lastTests) {
        final PackageLoader packageLoader = new PackageLoader(maxPackage.getClass().getClassLoader(), Classpath.fromSystem());
        for (Class javaClass : packageLoader.load(maxPackage, false)) {
            if (!Modifier.isAbstract(javaClass.getModifiers()) &&  TestCase.class.isAssignableFrom(javaClass) && !Arrays.contains(lastTests, javaClass)) {
                final Class<Class<? extends TestCase>> type = null;
                testClasses.add(StaticLoophole.cast(type, javaClass));
            }
        }
        for (Class javaClass : lastTests) {
            if (!Modifier.isAbstract(javaClass.getModifiers()) &&  TestCase.class.isAssignableFrom(javaClass)) {
                final Class<Class<? extends TestCase>> type = null;
                testClasses.add(StaticLoophole.cast(type, javaClass));
            } else {
                ProgramWarning.message("explicitly specified class is not a valid JUnit test class: " + javaClass);
            }
        }
    }
}
