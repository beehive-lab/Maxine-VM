/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.examples.vectorlib;

import com.oracle.max.graal.graph.Node.NodePhase;

public class Main {
    public static final int N = 1000000;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        Point p1 = new Point(5, 4);
        Point p2 = new Point(3, 2);
        System.out.println("result=" + testVector(p1, p2));
        System.out.println("time=" + (System.currentTimeMillis() - start) + "ms");
    }

    @NodePhase(PointOptimizationPhase.class)
    public static int testVector(Point p1, Point p2) {
        int result = 0;
        for (int i = 0; i < N; ++i) {
            if (p1.same(p1)) {
                result++;
            }
            if (p1.same(p2)) {
                result++;
            }
        }
        return result;
    }
}
