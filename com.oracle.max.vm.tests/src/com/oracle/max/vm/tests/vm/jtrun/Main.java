/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.vm.tests.vm.jtrun;

import com.oracle.max.vm.tests.vm.jtrun.all.JTRuns;

/**
 * Simple class to allow a main entry point into the Java tester tests.
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
