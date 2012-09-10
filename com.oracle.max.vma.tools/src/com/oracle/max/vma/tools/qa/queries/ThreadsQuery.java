/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.oracle.max.vma.tools.qa.*;


public class ThreadsQuery extends QueryBase {
    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        for (Map.Entry<String, ThreadRecord> entry : traceRun.threads.entrySet()) {
            String name = entry.getKey();
            ThreadRecord threadRecord = entry.getValue();
            ps.print(name);
            ps.print(", start time ");
            if (threadRecord.startTime > 0) {
                ps.print(TimeFunctions.formatTime(timeValue(traceRun, threadRecord.startTime)));
            } else {
                ps.print("unknown");
            }
            ps.print(", end time ");
            if (threadRecord.endTime > 0) {
                ps.print(TimeFunctions.formatTime(timeValue(traceRun, threadRecord.endTime)));
            } else {
                ps.print("unknown");
            }
            ps.println();
        }
        return null;
    }

}
