/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.jtrun;

import java.util.*;

import com.sun.max.vm.*;

/**
 * The {@code JTUtil} class definition.
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
