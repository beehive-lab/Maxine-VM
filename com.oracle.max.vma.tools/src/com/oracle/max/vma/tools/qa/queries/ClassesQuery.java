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
 * Breaks down the instances by class/classloader, showing the number of instances of
 * allocated by each class/classloader combination. The option <code>-sortbycount</code>
 * generates the data sorted by the number of instances (default alphabetic by class).
 *
 */
public class ClassesQuery extends QueryBase {

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        boolean sortByCount = false;
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-sortbycount")) {
                sortByCount = true;
            }
        }
        for (int i = 0; i < traceRuns.size(); i++) {
            if (traceFocus < 0 || i == traceFocus) {
                TraceRun traceRun = traceRuns.get(i);
                int longestClassName = 0;
                Iterator<ClassRecord> iter = traceRun.getClassesIterator();
                final ArrayList<ClassAndCount> classArrayList = new ArrayList<ClassAndCount>();
                while (iter.hasNext()) {
                    final ClassRecord cr = iter.next();
                    if (classMatches(cr)) {
                        final int length = cr.getName().length();
                        if (length > longestClassName) {
                            longestClassName = length;
                        }
                        classArrayList.add(new ClassAndCount(cr, cr.getObjects().size()));
                    }
                }
                ClassAndCount[] classArray = new ClassAndCount[classArrayList.size()];
                classArrayList.toArray(classArray);
                if (sortByCount) {
                    Arrays.sort(classArray, new ClassAndCount(null, 0));
                }
                ps.print("Instances    Class");
                space(ps, longestClassName - 5 + 2);
                ps.println(" Classloader ");
                for (ClassAndCount cc : classArray) {
                    ps.format("%10d   %s", cc.count, cc.classRecord.getName());
                    space(ps, longestClassName + 3 - cc.classRecord.getName().length());
                    ps.println(getShowClassLoader(traceRun, cc.classRecord.getClassLoaderId()));
                }
            }
        }
        return null;
    }

    static class ClassAndCount implements Comparator<ClassAndCount> {
        ClassRecord classRecord;
        int count;

        ClassAndCount(ClassRecord classRecord, int count) {
            this.classRecord = classRecord;
            this.count = count;
        }

        public int compare(ClassAndCount c1, ClassAndCount c2) {
            return c1.count > c2.count ? -1 : c1.count == c2.count ? 0 : 1;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof ClassAndCount) && ((ClassAndCount) obj).count == count;
        }
    }
}
