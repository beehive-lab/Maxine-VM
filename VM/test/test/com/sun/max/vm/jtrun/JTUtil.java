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
package test.com.sun.max.vm.jtrun;

import com.sun.max.vm.Log;

/**
 * The <code>JTUtil</code> class definition.
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
    protected static String lastTestName;

    public static void reset(int start, int end) {
        testNum = start;
        total = end - start;
        passed = 0;
        failed = 0;
        finished = 0;
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
        Log.println(".");
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
        if (verbose == 2) {
            verbose(false, finished, total);
        }
        if (verbose == 3) {
            printRun(run);
            Log.println(" failed with incorrect result");
        }
        testNum++;
    }

    public static void fail(String run, Throwable t) {
        failed++;
        finished++;
        if (verbose == 2) {
            verbose(false, finished, total);
        }
        if (verbose == 3) {
            printRun(run);
            Log.print(" failed with exception !");
            Log.println(t.getClass().getName());
        }
        testNum++;
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
        if (verbose == 3) {
            printTestNum();
            Log.print(test);
            lastTestName = test;
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
