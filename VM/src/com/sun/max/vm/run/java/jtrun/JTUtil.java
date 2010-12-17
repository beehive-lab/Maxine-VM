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
package com.sun.max.vm.run.java.jtrun;

import com.sun.max.vm.Log;

import java.util.List;
import java.util.LinkedList;

/**
 * The {@code JTUtil} class definition.
 *
 * @author Ben L. Titzer
 */
public class JTUtil {
    public static int passed;
    public static int failed;
    public static int finished;
    public static int total;
    public static int testNum;
    public static int verbose = 2;
    public static boolean recordFailures = true;

    protected static String lastTestName;

    protected static List<String> failures;

    public static void reset(int start, int end) {
        testNum = start;
        total = end - start;
        lastTestName = null;
        passed = 0;
        failed = 0;
        finished = 0;
        failures = null;
    }

    public static void printReport() {
        Log.print("Done: ");
        Log.print(passed);
        Log.print(" of ");
        Log.print(finished);
        Log.print(" passed");
        if (failed > 0) {
            Log.print(" (");
            Log.print(failed);
            Log.print(" failed)");
        }
        Log.println("");
        if (failures != null) {
            for (String f : failures) {
                Log.println(f);
            }
        }
    }

    public static void pass() {
        passed++;
        finished++;
        if (verbose == 2) {
            verbose(true, finished, total);
        }
        testNum++;
    }

    public static void fail(String run) {
        failed++;
        finished++;
        recordFailure(run, null);
        if (verbose == 3) {
            printRun(run);
            Log.println(" failed with incorrect result");
        }
        testNum++;
    }

    public static void fail(String run, Throwable t) {
        failed++;
        finished++;
        recordFailure(run, t);
        if (verbose == 3) {
            printRun(run);
            Log.print(" failed with exception !");
            Log.println(t.getClass().getName());
            if (verbose == 4) {
                t.printStackTrace(Log.out);
            }
        }
        testNum++;
    }

    private static void recordFailure(String run, Throwable t) {
        if (verbose == 2) {
            verbose(false, finished, total);
        }
        if (recordFailures) {
            if (failures == null) {
                failures = new LinkedList<String>();
            }
            StringBuilder b = new StringBuilder();
            b.append(testNum);
            b.append(": ");
            if (lastTestName != null) {
                b.append(lastTestName);
            }
            if (run != null) {
                b.append(".test");
                b.append(run);
            }
            if (t == null) {
                b.append(" failed with incorrect result");
            } else {
                b.append(" failed with exception !");
                b.append(t.getClass().getName());
            }
            failures.add(b.toString());
        }
    }

    public static void printRun(String run) {
        Log.print("\t");
        printTestNum();
        if (lastTestName != null) {
            Log.print(lastTestName);
        }
        if (run != null) {
            Log.print(".test");
            Log.print(run);
        }
    }

    public static void verbose(boolean passed, int finished, int total) {
        Log.print(passed ? '.' : 'X');
        if (finished % 10 == 0) {
            Log.print(' ');
        }
        if (finished % 50 == 0) {
            Log.print(' ');
            Log.print(finished);
            Log.print(" of ");
            Log.println(total);
        } else if (finished == total) {
            Log.println();
        }
        Log.flush();
    }

    public static void begin(String test) {
        lastTestName = test;
        if (verbose == 3) {
            printTestNum();
            Log.print(test);
            int i = test.length();
            while (i++ < 50) {
                Log.print(' ');
            }
            Log.println("  next: -XX:TesterStart="  + (testNum + 1));
        }
    }

    public static void printTestNum() {
        // print out the test number (aligned to the left)
        Log.print(testNum);
        Log.print(':');
        if (testNum < 100) {
            Log.print(' ');
        }
        if (testNum < 10) {
            Log.print(' ');
        }
        Log.print(' ');
    }
}
