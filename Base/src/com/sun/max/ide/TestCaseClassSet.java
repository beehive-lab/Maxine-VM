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
package com.sun.max.ide;

import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * A utility for defining and refining a set of {@linkplain #isJUnitTestCaseClass(Class) valid} JUnit test case classes.
 *
 * @author Doug Simon
 */
public class TestCaseClassSet extends LinkedHashSet<Class<? extends TestCase>> {

    private final String _defaultTestSuiteName;

    /**
     * Creates a set of classes whose {@linkplain #toTestSuite() derived} test suite will have a given name.
     *
     * @param defaultTestSuiteName the default name to be used for the test suite derived from this set
     */
    public TestCaseClassSet(String defaultTestSuiteName) {
        _defaultTestSuiteName = defaultTestSuiteName;
    }

    /**
     * Creates a set of classes by scanning a given package for {@linkplain #isJUnitTestCaseClass(Class) valid} JUnit
     * test case classes.
     *
     * @param maxPackage the package to scan for classes
     * @param scanSubPackages specifies if the sub-packages of {@code maxPackage} should also be scanned
     */
    public TestCaseClassSet(MaxPackage maxPackage, boolean scanSubPackages) {
        _defaultTestSuiteName = maxPackage.name();
        add(maxPackage);
        if (scanSubPackages) {
            for (MaxPackage subPackage : maxPackage.getTransitiveSubPackages(Classpath.fromSystem())) {
                add(subPackage);
            }
        }
    }

    /**
     * Creates a set of classes by scanning a given package (but not its sub-packages) for
     * {@linkplain #isJUnitTestCaseClass(Class) valid} JUnit test case classes.
     *
     * @param maxPackage the package to scan for classes
     */
    public TestCaseClassSet(MaxPackage maxPackage) {
        this(maxPackage, false);
    }

    public static boolean isJUnitTestCaseClass(Class javaClass) {
        return javaClass != null && !Modifier.isAbstract(javaClass.getModifiers()) &&  TestCase.class.isAssignableFrom(javaClass);
    }

    /**
     * Adds all the classes in {@code maxPackage} that subclass {@link TestCase} to this set.
     *
     * @param maxPackage the package to scan for classes
     */
    public TestCaseClassSet add(MaxPackage maxPackage) {
        final PackageLoader packageLoader = new PackageLoader(maxPackage.getClass().getClassLoader(), Classpath.fromSystem());
        for (Class javaClass : packageLoader.load(maxPackage, false)) {
            if (isJUnitTestCaseClass(javaClass)) {
                final Class<Class<? extends TestCase>> type = null;
                add(StaticLoophole.cast(type, javaClass));
            }
        }
        return this;
    }

    /**
     * Adds or moves a given set of classes to this set such that they will be returned after all existing entries
     * when this set is iterated over.
     *
     * @param classes the classes to add
     */
    public TestCaseClassSet addToEnd(Class... classes) {
        for (int i = classes.length - 1; i >= 0; --i) {
            final Class c = classes[i];
            if (isJUnitTestCaseClass(c)) {
                remove(c);
                final Class<Class<? extends TestCase>> type = null;
                add(StaticLoophole.cast(type, c));
            } else {
                ProgramWarning.message("Class is not an instantiable subclass of TestCase: " + c);
            }
        }
        return this;
    }

    /**
     * Removes a given set of classes from this set.
     *
     * @param classes the classes to remove
     */
    public TestCaseClassSet removeAll(Class... classes) {
        removeAll(java.util.Arrays.asList(classes));
        return this;
    }

    /**
     * Creates a test suite containing the tests defined by the classes in this set.
     *
     * @param name the name of the suite
     * @return the created suite
     */
    public TestSuite toTestSuite(String name) {
        final TestSuite suite = new TestSuite(name);
        for (Class<? extends TestCase> testClass : this) {
            suite.addTestSuite(testClass);
        }
        return suite;
    }

    public TestSuite toTestSuite() {
        return toTestSuite(_defaultTestSuiteName);
    }
}
