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
package jtt.except;

/*
 * @Harness: java
 * @Runs: 0=0; 1=0; -2=-1; 3=0
 */
public class StackTrace_NPE_03 {

    private static String[] trace = {"test2", "test1", "test"};

    public static int test(int a) {
        try {
            if (a >= 0) {
                return test1();
            }
        } catch (NullPointerException npe) {
            String thisClass = StackTrace_NPE_03.class.getName();
            StackTraceElement[] stackTrace = npe.getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement e = stackTrace[i];
                if (e.getClassName().equals(thisClass)) {
                    for (int j = 0; j < trace.length; j++) {
                        StackTraceElement f = stackTrace[i + j];
                        if (!f.getClassName().equals(thisClass)) {
                            return -2;
                        }
                        if (!f.getMethodName().equals(trace[j])) {
                            return -3;
                        }
                    }
                    return 0;
                }
            }
        }
        return -1;
    }

    private static int test1() {
        return test2();
    }

    private static int test2() {
        throw new NullPointerException();
    }
}