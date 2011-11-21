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

import java.io.PrintStream;
import java.util.*;

import com.oracle.max.vma.tools.qa.*;


/**
 * Lists the objects allocated by a given thread.
 *
 * Optional arguments:
 * <ul>
 * <li><code>-summary</code>: only list total allocated/live objects.
 * <li><code>-sort</code>: sort instance by lifetime.
 * </ul>
 */
public class DataByThreadQuery extends QueryBase {

    private boolean summary;
    private boolean sort_lt;
    private boolean sort_mlt;

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        parseArgs(args);
        TraceRun traceRun = traceRuns.get(traceFocus);
        Map<String, ArrayList<ObjectRecord>> threadMap = DataByThreadQueryHelper.getObjectsByThread(traceRun);

        for (Map.Entry<String, ArrayList<ObjectRecord>> me : threadMap.entrySet()) {
            if (thread == null || thread.equals(me.getKey())) {
                ps.println("Objects allocated by thread " + me.getKey());
                ArrayList<ObjectRecord> thObjects = me.getValue();
                int totalLiveObjects = 0;
                ObjectRecord[] thObjectsArray = thObjects.toArray(new ObjectRecord[thObjects.size()]);
                if (sort_lt) {
                    SortUtil.sortByLifeTime(thObjectsArray, 0, traceRun);
                } else if (sort_mlt) {
                    SortUtil.sortByModifyLifeTime(thObjectsArray, 0);
                }
                for (int i = 0; i < thObjectsArray.length; i++) {
                    ObjectRecord td = thObjectsArray[i];
                    if (!summary) {
                        ps.println(td.toString(traceRun, true, false, true, true, true, true));
                    }
                    if (td.getDeletionTime() == 0) {
                        totalLiveObjects++;
                    }
                }
                ps.println("Total objects allocated: " + thObjects.size());
                ps.println("Total live objects: " + totalLiveObjects + "\n");
            }
        }
        return null;
    }

    private void parseArgs(String[] args) {
        summary = false;
        sort_lt = false;
        sort_mlt = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-summary")) {
                summary = true;
            } else if (arg.equals("-sort_lt")) {
                sort_lt = true;
            } else if (arg.equals("-sort_mlt")) {
                sort_mlt = true;
            }
        }
    }

}
