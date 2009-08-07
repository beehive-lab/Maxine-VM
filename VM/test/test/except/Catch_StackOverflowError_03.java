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
package test.except;

/** Some basic checking of the stack trace produced after a StackOverflowError.
 * @author Paul Caprioli
 * @Harness: java
 * @Runs: 0 = 0;
 */
public class Catch_StackOverflowError_03 {

    private static final int MIN_RECURSION_DEPTH = 16;
    private static final int EXTRA_DEPTH_PRINT = 8;

    private static void recurseA() {
        recurseB();
    }

    private static void recurseB() {
        recurseA();
    }

    public static int test(int ignore) {
        try {
            recurseA();
        } catch (StackOverflowError stackOverflowError) {
            // Check that a method does not appear to be calling itself in the stack trace:
            StackTraceElement[] elements = stackOverflowError.getStackTrace();
            String lastMethodName = "";
            for (int i = 0; i < elements.length; ++i) {
                String methodName = elements[i].getMethodName();
                if (i < MIN_RECURSION_DEPTH && !methodName.startsWith("recurse") ||
                                lastMethodName.equals(methodName)) {
                    for (int j = 0; j < elements.length && j < i + EXTRA_DEPTH_PRINT; ++j) {
                        System.err.println(elements[j]);
                    }
                    System.err.println("....");
                    return 1;
                }
                lastMethodName = methodName;
            }
        }
        return 0;
    }
}
