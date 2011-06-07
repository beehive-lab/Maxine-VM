/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Query checks whether any object allocated by a thread is accessed by another thread.
 */

import java.io.PrintStream;
import java.util.*;

/**
 * Analyses the trace to determine what objects are/are not thread local.
 * N.B. This requires that the trace was generated with read tracking.
 */

import com.oracle.max.vma.tools.qa.*;

public class ThreadLocalQuery extends QueryBase {

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        Map<String, ArrayList<ObjectRecord>> threadMap = DataByThreadsQueryHelper.getObjectsByThread(traceRun);

        for (Map.Entry<String, ArrayList<ObjectRecord>> me : threadMap.entrySet()) {
            ps.println("Check objects allocated by thread " + me.getKey());
            ArrayList<ObjectRecord> thObjects = me.getValue();
            for (ObjectRecord obj : thObjects) {
                final Set<String> threads = accessedByAnotherThread(obj);
                if (threads != null) {
                    for (String thread : threads) {
                        ps.println("object " + obj.getId() + " is accessed by thread " + thread);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if a given object is accessed by any other thread.
     * @param obj
     * @return
     */
    private static Set<String> accessedByAnotherThread(ObjectRecord obj) {
        HashSet<String> result = null;
        final String threadId = obj.getThreadId();
        for (ObjectRecord.TraceElement te : obj.getTraceElements()) {
            if (!te.getThreadId().equals(threadId)) {
                if (result == null) {
                    result = new HashSet<String>();
                }
                result.add(te.getThreadId());
            }
        }
        return result;
    }
}
