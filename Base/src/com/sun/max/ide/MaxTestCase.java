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

import junit.framework.*;

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
}
