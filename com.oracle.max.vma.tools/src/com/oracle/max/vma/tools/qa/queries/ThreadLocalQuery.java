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

import static com.oracle.max.vma.tools.qa.AdviceRecordHelper.*;
import java.io.PrintStream;
import java.util.*;

/**
 * Analyses the trace to determine what objects are/are not thread local.
 * N.B. This requires that the trace was generated with read tracking.
 */

import com.oracle.max.vma.tools.qa.*;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * Checks whether objects allocated by one thread are ever accessed by another thread.
 * Note this detects actual access in the sense of using the object (e.g., invokinmg a method, checking its type,
 * accessing a field) and not just acquiring a reference (direct or indirect) to the object.
 */
public class ThreadLocalQuery extends QueryBase {

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        Map<String, ArrayList<ObjectRecord>> threadMap = DataByThreadQueryHelper.getObjectsByThread(traceRun);

        for (Map.Entry<String, ArrayList<ObjectRecord>> me : threadMap.entrySet()) {
            if (thread == null || thread.equals(me.getKey())) {
                ps.println("Check objects allocated by thread " + me.getKey());
                ArrayList<ObjectRecord> thObjects = me.getValue();
                for (ObjectRecord obj : thObjects) {
                    final Set<ThreadRecord> threads = accessedByAnotherThread(obj);
                    if (threads != null) {
                        for (ThreadRecord thread : threads) {
                            ps.printf("object %s created by '%s' is accessed by '%s'%n", obj, obj.thread, thread);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if a given object is accessed by any other thread.
     * @param obj
     */
    private static Set<ThreadRecord> accessedByAnotherThread(ObjectRecord obj) {
        HashSet<ThreadRecord> result = null;
        for (AdviceRecord ar : obj.getAdviceRecords()) {
            if (!(ar.thread == obj.thread)) {
                if (result == null) {
                    result = new HashSet<ThreadRecord>();
                }
                result.add(getThread(ar));
            }
        }
        return result;
    }
}
