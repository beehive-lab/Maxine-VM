/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.max.vma.tools.qa.AdviceRecordHelper.*;
import com.oracle.max.vma.tools.qa.TransientVMAdviceHandlerTypes.*;

/**
 * List the mutable objects for all classes or a specific class.
 *
 * Args:
 * <ul>
 * <li><code>-class name</code>: limit output to class name.
 * </ul>
 */

public class MutableObjectsQuery extends QueryBase {

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);

        Iterator<ClassRecord> iter = traceRun.getClassesIterator();
        while (iter.hasNext()) {
            ClassRecord cr = iter.next();
            if (classMatches(cr)) {
                ArrayList<ObjectRecord> a = cr.getObjects();
                ps.println("Mutable objects for class " + cr.getName());
                for (int i = 0; i < a.size(); i++) {
                    ObjectRecord td = a.get(i);
                    if (td.getModifyLifeTime() > 0) {
                        ps.printf("  %s%n", td.toString(traceRun, false, false, true, true, true, true));
                        for (int j = 0; j < td.getAdviceRecords().size(); j++) {
                            AdviceRecord ar = td.getAdviceRecords().get(j);
                            if (AdviceRecordHelper.accessType(ar) == AccessType.WRITE && ar.time > td.getEndCreationTime()) {
                                ObjectFieldAdviceRecord far = (ObjectFieldAdviceRecord) ar;
                                ps.println("    field '" + ((FieldRecord) far.field).getName() + "' modified at " + ms(traceRun.relTime(ar.time)));
                            }
                        }
                    }
                }
                if (className != null) {
                    break;
                }
            }
        }
        return null;
    }
}

