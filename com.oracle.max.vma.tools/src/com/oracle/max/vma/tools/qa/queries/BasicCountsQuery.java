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

import java.io.*;
import java.util.*;

import com.oracle.max.vma.tools.qa.*;

/**
 * Outputs basic information on the trace. Displays the number of classes, classloaders, objects, arrays, the number of
 * missing constructors.
 */
public class BasicCountsQuery extends QueryBase {

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        ps.println("Classes: " + getNumClasses(traceRun));
        ps.println("ClassLoaders: " + traceRun.getClassLoaders().size());
        long objs = traceRun.getObjectCount();
        long arrays = traceRun.getArrayCount();
        ps.format("Instances: %d (Arrays: %d, Non-Arrays: %d)\n", objs + arrays, arrays, objs);
        ps.println("Missing Constructors: " + traceRun.getMissingConstructorCount());
        return null;
    }

    private int getNumClasses(TraceRun traceRun) {
        int result = 0;
        Iterator<ClassRecord> iter = traceRun.getClassesIterator();
        while (iter.hasNext()) {
            iter.next();
            result++;
        }
        return result;
    }
}

