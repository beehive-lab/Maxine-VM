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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.io.PrintStream;
import java.net.*;

/**
 * Base class for query implementations that defines some useful methods.
 *
 */

public abstract class QueryBase {
    protected final static String INDENT_TWO = "  ";
    protected final static String INDENT_FOUR = "    ";
    protected final static String DEFAULT_QUERY_PACKAGE = "com.oracle.max.vma.tools.qa.queries";

    protected static DecimalFormat format6 = new DecimalFormat("#.000000");
    protected static DecimalFormat format4 = new DecimalFormat("#.0000");
    protected static DecimalFormat format2 = new DecimalFormat("#.00");
    protected static FieldPosition fpos0 = new FieldPosition(0);

    private static ArrayList<URL> queryClassURLs = new ArrayList<URL>();
    private static URLClassLoader urlClassLoader;
    private static String packageName = DEFAULT_QUERY_PACKAGE;

    protected boolean verbose;
    protected String className;
    protected String id;
    protected String thread;
    protected boolean absTime = false;

    public static void addQueryClassDir(String dir) {
        try {
            queryClassURLs.add(new URL(dir));
        } catch (Exception e) {
            throw new RuntimeException("malformed class directory " + dir);
        }
    }

    public static void setQueryPackage(String p) {
        packageName = p;
    }

    public static QueryBase ensureLoaded(String queryName) {
        try {
            if (urlClassLoader == null) {
                urlClassLoader  = new URLClassLoader(queryClassURLs.toArray(new URL[queryClassURLs.size()]));
            }
            Class<?> q = urlClassLoader.loadClass(packageName + "." + queryName + "Query");
            QueryBase query = (QueryBase) q.newInstance();
            return query;
        } catch (Exception e) {
            throw new RuntimeException("failed to load query class " + queryName);
        }
    }

    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        ps.println("no query defined");
        return null;
    }

    public void parseStandardArgs(String[] args) {
        verbose = false;
        className = null;
        id = null;
        thread = null;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-class") || arg.equals("-c"))  {
                className = args[++i];
            } else if (arg.equals("-thread") || arg.equals("-t"))  {
                thread = args[++i];
            } else if (arg.equals("-id")) {
                id = args[++i];
            } else if (arg.equals("-abs")) {
                absTime = true;
            }
        }
        // Checkstyle: resume modified control variable check
    }

    public static double ms(long t) {
        return  ((double) t) / 1000000;
    }

    public static double percent(long a, long b) {
        if (a == 0) {
            return 0.0;
        } else if (b == 0) {
            return 100.0;
        } else {
            return (double) (a * 100) / (double) b;
        }
    }

    public static String d6d(double d) {
        return format6.format(d, new StringBuffer(), fpos0).toString();
    }

    public static String d4d(double d) {
        return format4.format(d, new StringBuffer(), fpos0).toString();
    }

    public static String d2d(double d) {
        return format2.format(d, new StringBuffer(), fpos0).toString();
    }

    public void space(PrintStream ps, int n) {
        for (int i = 0; i < n; i++) {
            ps.print(' ');
        }
    }

    public static String getShowClassLoader(TraceRun traceRun, String classLoaderId) {
        final String defaultName = classLoaderId;
        ObjectRecord ctd = traceRun.objects.get(defaultName);
        if (ctd == null) {
            return "id=" + defaultName;
        } else {
            return ctd.getClassName();
        }
    }
}
