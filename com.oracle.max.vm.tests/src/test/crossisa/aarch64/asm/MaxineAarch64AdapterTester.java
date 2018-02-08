/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package test.crossisa.aarch64.asm;


/**
 * JUnit test harness for aarch64 stack frame adapters.
 *
 */
public class MaxineAarch64AdapterTester extends MaxineAarch64Tester {

    public MaxineAarch64AdapterTester(String[] args) {
        super(args);
    }

    public MaxineAarch64AdapterTester(long[] expected, boolean[] test, BitsFlag[] range) {
        super(expected, test, range);
    }

    @Override
    public void run() throws Exception {
        assemble("prelude.s", "prelude.o");
        assemble("method.s", "method.o");
        assemble("adapter.s", "adapter.o");
        assembleStartup();
        link("test_aarch64_adapters.ld", "prelude.o", "method.o", "adapter.o", "startup_aarch64.o");
        runSimulation();
    }

    public static void main(String[] args) throws Exception {
        MaxineAarch64AdapterTester tester = new MaxineAarch64AdapterTester(args);
        tester.run();
    }

}
