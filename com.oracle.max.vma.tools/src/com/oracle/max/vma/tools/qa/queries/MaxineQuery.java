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

import static com.oracle.max.vma.tools.qa.AdviceRecordHelper.*;

import java.io.*;
import java.util.*;

import com.oracle.max.vma.tools.qa.*;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * A query to discover what parts of the recorded data refers to objects defined by Maxine, that is, in the VM
 * classloader, owing to Maxine's internal use of JDK classes that might have been instrumented.
 */
public class MaxineQuery extends QueryBase {
    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus,
            PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        ArrayList<Integer> vmIndices = new ArrayList<Integer>();

        boolean detail = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-detail")) {
                detail = true;
            }
        }

        String vmClId = null;
        for (String clId : traceRun.classLoaders.keySet()) {
            String clName = getShowClassLoader(traceRun, clId);
            if (clName.contains("VMClassLoader")) {
                vmClId = clId;
                break;
            }
        }

        if (vmClId == null) {
            ps.println("no Maxine objects");
            return null;
        }

        for (int i = 0; i < traceRun.adviceRecordList.size(); i++) {
            AdviceRecord ar = traceRun.adviceRecordList.get(i);
            if (ar instanceof ObjectObjectAdviceRecord) {
                ObjectObjectAdviceRecord ooar = (ObjectObjectAdviceRecord) ar;
                if (checkVM(vmClId, getObjectRecord(ooar))) {
                    vmIndices.add(i);
                } else {
                    if (ooar.value2 instanceof ObjectRecord) {
                        ObjectRecord or2 = (ObjectRecord) ooar.value2;
                        if (checkVM(vmClId, or2)) {
                            vmIndices.add(i);
                        }
                    }
                }
            } else if (ar instanceof ObjectAdviceRecord) {
                ObjectAdviceRecord oar = (ObjectAdviceRecord) ar;
                if (oar.value instanceof ObjectRecord) {
                    if (checkVM(vmClId, getObjectRecord(oar))) {
                        vmIndices.add(i);
                    }
                }
            }
        }

        ps.printf("%d records involve VM objects%n", vmIndices.size());
        if (detail) {
            for (int index : vmIndices) {
                print(this, traceRun, ps, traceRun.adviceRecordList.get(index), traceRun.adviceRecordList, 0, true);
            }
        }
        return null;
    }

    private boolean checkVM(String vmClId, ObjectRecord objectRecord) {
        return objectRecord != null && objectRecord.getClassLoaderId().equals(vmClId);
    }
}
