/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.max.vma.tools.qa.AllocationEpoch.*;

import java.io.PrintStream;
import java.util.ArrayList;

import com.oracle.max.vma.tools.qa.*;

public class GCQuery extends QueryBase {
    private boolean showRemovals;

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        parseArgs(args);
        TraceRun traceRun = traceRuns.get(traceFocus);
        ps.println("Allocation epochs");
        AllocationEpoch prev = null;
        for (AllocationEpoch gce : traceRun.allocationEpochs) {
            if (prev != null) {
                ps.println("GC time " + TimeFunctions.formatTime(gce.startTime - prev.endTime));
            }
            if (absTime) {
                ps.println(gce);
            } else {
                long end = gce.endTime;
                long start = gce.startTime;
                ps.println(gce.toString(traceRun.relTime(start), traceRun.relTime(end)));
            }
            prev = gce;
            if (showRemovals) {
                RemovalRange rr = gce.getRemovalRange();
                if (rr != null) {
                    int charCount = 0;
                    ps.print("  Objects collected at end of epoch");
                    for (int i = rr.startRemovalRange; i <= rr.endRemovalRange; i++) {
                        ObjectRecord or = AdviceRecordHelper.getObjectRecord(traceRun.adviceRecordList.get(i));
                        String ors = or.toString();

                        if (charCount + ors.length() > 80 || i == rr.startRemovalRange) {
                            ps.println();
                            ps.print("  ");
                            charCount = 0;
                        } else {
                            ps.print(' ');
                        }
                        charCount += ors.length();
                        ps.print(or);
                    }
                    ps.println();
                }
            }
        }
        return null;
    }

    private void parseArgs(String[] args) {
        for (String arg : args) {
            if (arg.equals("-r")) {
                showRemovals = true;
            }
        }
    }

}
