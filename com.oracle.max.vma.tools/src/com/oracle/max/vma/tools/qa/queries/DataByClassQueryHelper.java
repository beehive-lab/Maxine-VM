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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.oracle.max.vma.tools.qa.*;

/**
 * Helper class with common implementation of queries showing data by class
 *
 * The following arguments are interpreted by such queries:
 *
 * <ul>
 * <li><code>-class name</code>: restrict output to instances of class <code>name</code>, default all classes
 * <li><code>-sort_lt</code>: sort instances by lifetime (youngest to oldest)
 * <li><code>-sort_mlt</code>: sort instances by modified lifetime (shortest to longest)
 * </ul>.
 */
public class DataByClassQueryHelper extends QueryBase {
    static boolean sort_mlt;
    static boolean sort_lt;
    static boolean percent;
    static double percentile;
    static boolean summary;
    static boolean percentOnly;
    static boolean thread;
    static long ocount_g;
    static long pcount_g;
    static ArrayList<SummaryArrayElement> summaryArray;
    static ArrayList<Pattern> excludeList;

    protected static void showXDataByClasses(TraceRun traceRun, PrintStream ps,
            String[] args, Iterator<ClassRecord> classIter, String indent, boolean showCl) {
        String className = null;
        sort_mlt = false;
        sort_lt = false;
        percent = true;
        percentile = 100.0;
        summary = false;
        thread = false;
        percentOnly = false;
        ocount_g = pcount_g = 0;
        summaryArray = null;
        SummaryArrayElement.SortKey summaryArraySortKey = SummaryArrayElement.SortKey.Total;
        excludeList = new ArrayList<Pattern>();

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-class")) {
                i++;
                className = args[i];
            } else if (arg.equals("-sort_lt")) {
                sort_lt = true;
            } else if (arg.equals("-sort_mlt")) {
                sort_mlt = true;
            } else if (arg.equals("-abs")) {
                percent = false;
            } else if (arg.equals("-th")) {
                thread = true;
            } else if (arg.equals("-pci")) {
                i++;
                percentile = Double.parseDouble(args[i]);
            } else if (arg.equals("-summary")) {
                summary = true;
            } else if (arg.equals("-sort_summary")) {
                summary = true;
                summaryArray = new ArrayList<SummaryArrayElement>();
                i++;
                if (i < args.length) {
                    String k = args[i];
                    if (k.equals("class"))
                        summaryArraySortKey = SummaryArrayElement.SortKey.ClassName;
                    else if (k.equals("total"))
                        summaryArraySortKey = SummaryArrayElement.SortKey.Total;
                    else if (k.equals("imm_total"))
                        summaryArraySortKey = SummaryArrayElement.SortKey.ImmTotal;
                    else
                        i--;
                }
            } else if (arg.equals("-percent")) {
                summary = true;
                percentOnly = true;
            } else if (arg.equals("-exclude")) {
                i++;
                while (i < args.length) {
                    if (args[i].startsWith("-"))
                        break;
                    try {
                        excludeList.add(Pattern.compile(args[i]));
                    } catch (PatternSyntaxException ex) {
                        System.out.println("bad regex: " + args[i]);
                    }
                    i++;
                }
            }
        }
        // Checkstyle: resume modified control variable check

        ps.print(indent + "Objects organized by class");
        if (className != null) {
            ps.print(" (" + className + ")");
        }
        if (sort_lt) {
            ps.print(", sorted by lifetime");
        } else if (sort_mlt) {
            ps.print(", sorted by modify-lifetime");
        }
        ps.println("");

        while (classIter.hasNext()) {
            ClassRecord cr = classIter.next();
            if ((className == null) || cr.getName().equals(className)) {
                boolean excluded = false;
                for (Pattern pattern : excludeList) {
                    if (pattern.matcher(cr.getName()).matches()) {
                        System.out.println("excluding " + cr.getName());
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    showDataOnClass(traceRun, ps, cr, indent, showCl);
                }
            }
        }

        if (summary && className == null) {
            double pp = ((double) pcount_g / (double) ocount_g) * 100.0;
            ps.println("Total objects immutable for >= "
                    + TimeFunctions.ftime(percentile, TimeFunctions.format2d)
                    + ": " + pcount_g + " ("
                    + TimeFunctions.ftime(pp, TimeFunctions.format2d) + "%)");
        }

        if (summaryArray != null) {
            SummaryArrayElement[] saeArray = summaryArray.toArray(new SummaryArrayElement[summaryArray.size()]);
            Arrays.sort(saeArray, 0, saeArray.length,
                    new SummaryArrayElement.ThisComparator(summaryArraySortKey));
            ps.println("\nSummary sorted by " + summaryArraySortKey);
            ps.println("\nClass, Total, Total Immutable");
            for (SummaryArrayElement sae : saeArray) {
                ps.println(sae);
            }
        }
    }

    private static void showDataOnClass(TraceRun traceRun, PrintStream ps, ClassRecord cr,
            String indent, boolean showCl) {
        String className = cr.getName();
        ArrayList<ObjectRecord> objects = cr.getObjects();
        int ocount = objects.size(); ocount_g += ocount;
        int pcount = 0;
        if (!percentOnly) {
            ps.printf(indent + className  + ", total objects " + ocount + (showCl ? ", cl: " + traceRun.objects.get(cr.getClassLoaderId()) : ""));
        }
        ObjectRecord[] ods = objects.toArray(new ObjectRecord[objects.size()]);
        if (sort_lt) {
            SortUtil.sortByLifeTime(ods, 0, traceRun);
        } else if (sort_mlt) {
            SortUtil.sortByModifyLifeTime(ods, 0);
        }

        if (!summary) {
            ps.println("");
        }

        for (int i = 0; i < ods.length; i++) {
            ObjectRecord td = ods[i];
            long lifeTime = td.getLifeTime(traceRun.lastTime);
            long immutableTime = lifeTime - td.getModifyLifeTime();
            double percentImmutableTime = percent(immutableTime, lifeTime);
            if (percentImmutableTime >= percentile) {
                pcount++;
            }

            if (!summary && (percentImmutableTime <= percentile)) {
                String immutableForArg = percent ?
                    (TimeFunctions.ftime(percentImmutableTime, TimeFunctions.format6d) + "%") :
                    Long.toString(immutableTime);
                String threadArg = thread ? " th " + td.thread : "";
                ps.println(indent + INDENT_TWO + td.getId() +
                        threadArg +
                        ", ct " + TimeFunctions.formatTime(td.getEndCreationTime() - traceRun.startTime) +
                        ", lt " + TimeFunctions.formatTime(lifeTime) +
                        (td.getDeletionTime() == 0 ? ", alive" : ", dead") +
                        ", mlt " + TimeFunctions.formatTime(td.getModifyLifeTime()) +
                        ", immutable for " + immutableForArg);
            }
        }

        pcount_g += pcount;
        if (summaryArray != null) {
            summaryArray.add(new SummaryArrayElement(className, ocount, pcount));
        }

        if (!percentOnly && summary) {
            if (ocount > 0) {
                double pp = ((double) pcount / (double) ocount) * 100.0;
                ps.print(", objects immutable for >= " +
                        TimeFunctions.ftime(percentile, TimeFunctions.format2d) +
                        ": " + pcount +
                        " (" + TimeFunctions.ftime(pp, TimeFunctions.format2d) + "%)");
            }
            ps.println("");
        }
    }

    static class SummaryArrayElement {
        String className;
        long total;
        long immTotal;
        SummaryArrayElement(String className, long total, long immTotal) {
            this.className = className;
            this.total = total;
            this.immTotal = immTotal;
        }

        @Override
        public String toString() {
            return className + ", " + total + ", " + immTotal;
        }

        enum SortKey {ClassName, Total, ImmTotal}

        static class ThisComparator implements Comparator<SummaryArrayElement> {
            SortKey sortKey;
            ThisComparator(SortKey sortKey) {
                this.sortKey = sortKey;
            }

            public int compare(SummaryArrayElement a1, SummaryArrayElement a2) {
                switch (sortKey) {
                    case ClassName:
                        return a1.className.compareTo(a2.className);

                    case Total:
                        if (a1.total < a2.total) {
                            return 1;
                        } else if (a1.total > a2.total) {
                            return -1;
                        } else {
                            return 0;
                        }

                    case ImmTotal:
                        if (a1.immTotal < a2.immTotal) {
                            return 1;
                        } else if (a1.immTotal > a2.immTotal) {
                            return -1;
                        } else {
                            return 0;
                        }

                    default:
                        throw new IllegalArgumentException("impossible!");

                }
            }
        }
    }
}

