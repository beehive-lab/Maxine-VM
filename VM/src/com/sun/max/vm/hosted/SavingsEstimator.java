/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.sun.max.lang.*;

/**
 * This class is useful for prototyping refactorings aimed at reducing the size of the image.
 * An estimate for the amount of space saved can be computed after a graph has been built.
 *
 * @author Ben L. Titzer
 */
public class SavingsEstimator {
    final GraphStats graphStats;

    public SavingsEstimator(GraphStats graphStats) {
        this.graphStats = graphStats;
    }

    public void report(PrintStream printStream) {
        if (platform().wordWidth() == WordWidth.BITS_64) {
            reportCompressRefsSavings(printStream);
        }
    }

    private void reportCompressRefsSavings(PrintStream printStream) {
        Collection<GraphPrototype.ClassInfo> cstats = graphStats.graphPrototype.classInfos.values();
        printStream.println("Estimating savings from compressed references, ignoring alignment...");
        int totalRefs = 0;
        for (GraphPrototype.ClassInfo info : cstats) {
            final GraphStats.ClassStats s = GraphStats.getClassStats(info);
            int instanceRefs = info.instanceFields.size();
            int staticRefs = info.staticFields.size();
            int savedRefs = instanceRefs * s.objectCount + staticRefs;
            if (savedRefs > 0) {
                // save instance refs and static refs
//                printStream.println(info.clazz + " " + instanceRefs + " x " + s.objectCount + " " + staticRefs + " = " + savedRefs);
                totalRefs += savedRefs;
            }
        }

        for (Object o : graphStats.graphPrototype.objects) {
            totalRefs += 2; // two words per header
            if (o instanceof Object[]) {
                int length = ((Object[]) o).length;
                totalRefs += length;
            }
        }
        printStream.println("Estimated savings from compressed refs (" + totalRefs + " refs) = " + totalRefs * 4 + " bytes");
    }
}
