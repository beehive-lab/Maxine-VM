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
package jtt.optimize;

/*
 * Tests optimization of float operations.
 * @Harness: java
 * @Runs: 0d=22d; 1d=0d; 2d=144d; 3d=1d
 */
public class VN_Double01 {
    public static double test(double arg) {
        if (arg == 0) {
            return add(arg + 10);
        }
        if (arg == 1) {
            return sub(arg + 10);
        }
        if (arg == 2) {
            return mul(arg + 10);
        }
        if (arg == 3) {
	    System.out.println("ARG is 3");
            return div(arg + 10);
        }
        return 0;
    }
    public static double add(double x) {
        double c = 1;
        double t = x + c;
        double u = x + c;
        return t + u;
    }
    public static double sub(double x) {
        double c = 1;
        double t = x - c;
        double u = x - c;
        return t - u;
    }
    public static double mul(double x) {
        double c = 1;
        double t = x * c;
        double u = x * c;
        return t * u;
    }
    public static double div(double x) {
        double c = 1;
	System.out.println("c is " + c);
        double t = x / c;
	
	System.out.println("t is " + t);
	System.out.println("c is " + c);
	do {
	t = x / c;
	} while (t != 13.0);
        double u = x / c;
	System.out.println("x " + x + " t " + t + " u " + u + " t/u " + t/u);
	System.out.println("c is " + c);
	do{ 
		x = t/u;
	} while(x != 1.0);
        return t / u;
    }
}
