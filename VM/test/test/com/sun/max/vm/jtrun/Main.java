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

import test.com.sun.max.vm.jtrun.some.*;

/**
 * Simple class to allow a main entry point into the Java tester tests.
 *
 * @author Thomas Wuerthinger
 */
public class Main {

    /**
     * Call with start and end test number as parameters.
     */
    public static void main(String[] args) {
        int start = 0;
        int end = 10000;

        if (args.length > 0) {
            start = Integer.parseInt(args[0]);
        }

        if (args.length > 1) {
            end = Integer.parseInt(args[1]);
        }

        JTUtil.reset(start, end);
        JTUtil.verbose = 3;
        JTRuns.runTests(start, end);
        JTUtil.printReport();
    }
}
