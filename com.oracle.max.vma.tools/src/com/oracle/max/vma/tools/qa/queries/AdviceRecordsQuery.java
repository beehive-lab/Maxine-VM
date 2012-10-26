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

import java.io.*;
import java.util.*;

import com.oracle.max.vma.tools.qa.*;

public class AdviceRecordsQuery extends QueryBase {
    private static final String[] INDENTS = new String[64];

    static {
        String s = "";
        for (int i = 0; i < INDENTS.length; i++) {
            INDENTS[i] = s;
            s = s + "  ";
        }
    }

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        int fromIndex = 0;
        int toIndex = traceRun.adviceRecordList.size();
        boolean showIndex = false;
        boolean indenting = false;
        int indent = 0;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-from")) {
                fromIndex = Integer.parseInt(args[++i]);
            } else if (arg.equals("-to")) {
                toIndex = Integer.parseInt(args[++i]);
            } else if (arg.equals("-indent")) {
                indenting = true;
            } else if (arg.equals("-showindex")) {
                showIndex = true;
            }
        }
        // Checkstyle : resume modified control variable check
        long chunkStartTime = System.currentTimeMillis();
        long processStartTime = chunkStartTime;
        long count = 0;
        for (int index = fromIndex; index < toIndex; index++) {
            if (indenting) {
                ps.print(INDENTS[indent]);
            }
            indent = AdviceRecordHelper.print(this, traceRun, ps, traceRun.adviceRecordList.get(index), showIndex ? index : -1, indent, true);
            count++;
            if (verbose && ((count % 100000) == 0)) {
                long endTime = System.currentTimeMillis();
                System.out.printf("processed %d records in %d ms (%d)%n", count, endTime - processStartTime, endTime - chunkStartTime);
                chunkStartTime = endTime;
            }
        }
        return null;
    }

}
