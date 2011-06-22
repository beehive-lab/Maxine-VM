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

package com.oracle.max.vma.tools.qa;

/**
 * Misc sorting methods.
 *
 * @author Mick Jordan
 *
 */

import java.util.*;

public class SortUtil {
    public static void sortByLifeTime(ObjectRecord[] ods, int startIndex, TraceRun traceRun) {
        LifeTimeComparator lifeTimeComparator = new LifeTimeComparator(traceRun);
        Arrays.sort(ods, startIndex, ods.length, lifeTimeComparator);
    }

    public static void sortByModifyLifeTime(ObjectRecord[] ods, int startIndex) {
        ModifyLifeTimeComparator modifyLifeTimeComparator = new ModifyLifeTimeComparator();
        Arrays.sort(ods, startIndex, ods.length, modifyLifeTimeComparator);
    }



    public static class LifeTimeComparator implements Comparator<ObjectRecord> {
        private TraceRun traceRun;
        public LifeTimeComparator(TraceRun traceRun) {
            this.traceRun = traceRun;
        }

        public int compare(ObjectRecord od1, ObjectRecord od2) {
            long t1 = od1.getLifeTime(traceRun.lastTime);
            long t2 = od2.getLifeTime(traceRun.lastTime);
            if (t1 < t2) {
                return -1;
            } else if (t1 > t2) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("LifeTimeComparator.equals not implemented");
        }
    }
    public static class ModifyLifeTimeComparator implements Comparator<ObjectRecord> {
        public int compare(ObjectRecord od1, ObjectRecord od2) {
            long t1 = od1.getModifyLifeTime();
            long t2 = od2.getModifyLifeTime();
            if (t1 < t2) {
                return -1;
            } else if (t1 > t2) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("LifeTimeComparator.equals not implemented");
        }
    }
}
