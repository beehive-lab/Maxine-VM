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

package com.oracle.max.vma.tools.qa.queries;

import java.util.*;
import java.io.*;

import com.oracle.max.vma.tools.qa.*;

/**
 * For each class: first computes the immutable lifetime as a percentage, then
 * counts how many objects fall into buckets that are 1% in size.
 *
 * E.g.:
 *
 * Class sun.reflect.NativeMethodAccessorImpl, Object count 4 4: 1 (25.00) 42: 1
 * (25.00) 75: 1 (25.00) 100: 1 (25.00)
 *
 * This means that one object was immutable for 4%, one for 42%, one for 75% and
 * 1 for 100% of respective lifetime. The number in brackets is the percent of
 * the total for that bucket.
 */
public class ImmutableClassBucketsQuery extends QueryBase {
    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus,
            PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        Iterator<ClassRecord> iter = traceRun.getClassesIterator();
        while (iter.hasNext()) {
            ClassRecord cr = iter.next();
            if ((className == null) || cr.getName().equals(className)) {
                ArrayList<ObjectRecord> a = cr.getObjects();
                int ocount = a.size();
                double[] percents = new double[ocount];
                for (int i = 0; i < ocount; i++) {
                    ObjectRecord td = a.get(i);
                    long lifeTime = td.getEffectiveLifeTime();
                    percents[i] = percent(
                            lifeTime - td.getModifyLifeTime(), lifeTime);
                }
                Arrays.sort(percents);
                int[] buckets = new int[101];
                for (int k = 0; k < percents.length; k++) {
                    int p = (int) percents[k]; // either round or truncate
                    double pd = p;
                    if (pd > percents[k]) {
                        p--;
                    }
                    buckets[p]++;
                }
                ps.println("Class " + cr.getName() + ", Object count "
                        + ocount);
                for (int px = 0; px < buckets.length; px++) {
                    int bv = buckets[px];
                    if (bv != 0) {
                        ps.print("  " + px + ": " + bv + " ("
                                + d2d(percent(bv, ocount)) + "%)");
                    }
                }
                ps.println("");
            }
        }
        return null;
    }
}
