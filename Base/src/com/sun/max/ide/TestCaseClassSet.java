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

import static com.sun.max.lang.Classes.*;

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

    private final String defaultTestSuiteName;

    /**
     * Creates a set of classes whose {@linkplain #toTestSuite() derived} test suite will have a given name.
     *
     * @param defaultTestSuiteName the default name to be used for the test suite derived from this set
     */
    public TestCaseClassSet(String defaultTestSuiteName) {
        this.defaultTestSuiteName = defaultTestSuiteName;
    }

    /**
     * Creates a set of classes by scanning a given package for {@linkplain #isJUnitTestCaseClass(Class) valid} JUnit
     * test case classes.
     *
     * @param maxPackage the package to scan for classes
     * @param scanSubPackages specifies if the sub-packages of {@code maxPackage} should also be scanned
     */
//    public TestCaseClassSet(MaxPackage maxPackage, boolean scanSubPackages) {
//        this(maxPackage.name(), scanSubPackages);
//    }
//
//    /**
//     * Creates a set of classes by scanning a given package (but not its sub-packages) for
//     * {@linkplain #isJUnitTestCaseClass(Class) valid} JUnit test case classes.
//     *
//     * @param maxPackage the package to scan for classes
//     */
//    public TestCaseClassSet(MaxPackage maxPackage) {
//        this(maxPackage, false);
//    }

    public TestCaseClassSet(Class packageRepresentative) {
        this(packageRepresentative, false);
    }

    public TestCaseClassSet(Class packageRepresentative, boolean scanSubPackages) {
        this(getPackageName(packageRepresentative), scanSubPackages);
    }

    public TestCaseClassSet(final String packageName, final boolean scanSubPackages) {
        defaultTestSuiteName = packageName;
        new ClassSearch() {
            @Override
            protected boolean visitClass(boolean isArchiveEntry, String className) {
                if (!className.endsWith("package-info")) {
                    if (scanSubPackages || (Classes.getPackageName(className).equals(packageName))) {
                        Class javaClass = Classes.forName(className, false, getClass().getClassLoader());
                        if (isJUnitTestCaseClass(javaClass)) {
                            final Class<Class<? extends TestCase>> type = null;
                            add(Utils.cast(type, javaClass));
                        }
                    }
                }
                return true;
            }
        }.run(Classpath.fromSystem(), packageName.replace('.', '/'));
    }

    public static boolean isJUnitTestCaseClass(Class javaClass) {
        return javaClass != null && !Modifier.isAbstract(javaClass.getModifiers()) &&  TestCase.class.isAssignableFrom(javaClass);
    }

    /**
     * Adds or moves a given set of classes to this set such that they will be returned after all existing entries
     * when this set is iterated over.
     *
     * @param classes the classes to add
     */
    public TestCaseClassSet addToEnd(Class... classes) {
        for (int i = 0; i < classes.length; ++i) {
            final Class c = classes[i];
            if (isJUnitTestCaseClass(c)) {
                remove(c);
                final Class<Class<? extends TestCase>> type = null;
                add(Utils.cast(type, c));
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
        return toTestSuite(defaultTestSuiteName);
    }
}
