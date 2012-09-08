/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @Harness: java
 * @Runs: 0.4f = true
 */
package test.bench.bytecode;

import test.bench.util.*;

/**
 * A microbenchmark for floating point; {@code float} to {@code long} conversions.
 */
public class  F2L extends RunBench {

    protected F2L(float f) {
        super(new Bench(f));
    }

    public static boolean test(float f) {
        return new F2L(f).runBench();
    }

    static class Bench extends MicroBenchmark {
        private float f;
        Bench(float f) {
            this.f = f;
        }
        @Override
        public long run() {
            long l = (long) f;
            return l;
        }

        @SuppressWarnings("unused")
        private static void f2l(float d) {
            long i = (int) d;
        }

    }


    public static void main(String[] args) {
        test(0.4f);
    }

}
