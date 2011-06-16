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

        String className = null;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-class")) {
                className = args[++i];
            }
        }
        // Checkstyle: resume modified control variable check

        Iterator<ClassRecord> iter = traceRun.getClassesIterator();
        while (iter.hasNext()) {
            ClassRecord cr = iter.next();
            ArrayList<ObjectRecord> a = cr.getObjects();
            if ((className == null) || a.get(0).getClassName().equals(className)) {
                ps.println("Mutable objects for class " + a.get(0).getClassName());
                for (int i = 1; i < a.size(); i++) {
                    ObjectRecord td = a.get(i);
                    if (td.getModifyLifeTime() > 0) {
                        ps.println("Object " + td.getId());
                        for (int j = 0; j < td.getTraceElements().size(); j++) {
                            ObjectRecord.TraceElement te = td.getTraceElements().get(j);
                            if ((te instanceof ObjectRecord.WriteTraceElement) && (te.getAccessTime() > td.getEndCreationTime())) {
                                ps.println("  field " + te.getField() + " modified at " + ms(te.getAccessTime()));
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

