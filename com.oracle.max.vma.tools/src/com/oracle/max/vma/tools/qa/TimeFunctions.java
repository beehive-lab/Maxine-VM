/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vma.tools.qa;

import java.text.DecimalFormat;

/**
 * Misc. time functions.
 *
 * @author Mick Jordan
 *
 */

public class TimeFunctions {

    enum Format {
        Nano, Micro, Milli, Sec
    }

    private static Format timeFormat = Format.Milli;

    public static void setTimeFormat(Format tf) {
        timeFormat = tf;
    }

    public static DecimalFormat format2d = new DecimalFormat("#.##");
    public static DecimalFormat format3d = new DecimalFormat("#.###");
    public static DecimalFormat format6d = new DecimalFormat("#.######");
    public static DecimalFormat format9d = new DecimalFormat("#.#########");

    public static String formatTime(long time) {
        String ds = null;
        switch (timeFormat) {
            case Milli:
                ds = ftime(mtime(time), format6d) + "ms";
                break;
            case Micro:
                ds = ftime(utime(time), format3d) + "us";
                break;
            case Sec:
                ds = ftime(stime(time), format9d) + "s";
                break;
            case Nano:
        }
        return ds;
    }

    public static double utime(long time) {
        return (double) time / 1000;
    }

    public static double mtime(long time) {
        return (double) time / (1000 * 1000);
    }

    public static double stime(long time) {
        return (double) time / (1000 * 1000 * 1000);
    }

    public static String ftime(double time, DecimalFormat f) {
        return f.format(time);
    }
}

