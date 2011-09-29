/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.tree.*;

import com.oracle.max.vma.tools.qa.*;
import com.oracle.max.vma.tools.qa.callgraph.*;
import com.oracle.max.vma.tools.qa.callgraph.CallGraphDisplay.*;

/**
 * Assuming that the trace contains METHOD_ENTRY/RETURN records
 * constructs a call graph and displays it in a window.
 * Further queries can then be run on the call graph data.
 */
public class CallGraphQuery extends QueryBase {

    @Override
    public Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args) {
        TraceRun traceRun = traceRuns.get(traceFocus);
        boolean treePrint = false;
        boolean linearPrint = false;
        int maxDepth = Integer.MAX_VALUE;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-treeprint")) {
                args[i] = null;
                treePrint = true;
            } else if (arg.equals("-depth")) {
                args[i] = null;
                maxDepth = Integer.parseInt(args[++i]);
                args[i] = null;
            } else if (arg.equals("-linearprint")) {
                linearPrint = true;
                args[i] = null;
            }
        }
        // Checkstyle: resume modified control variable check
        CallGraphDisplay cg = CallGraphDisplay.queryMain(traceRun, removeProcessedArgs(args));
        CallGraphDisplay.timeDisplay = TimeDisplay.WallRel;

        if (treePrint) {
            for (DefaultMutableTreeNode root : cg.threadCallGraphs.values()) {
                MethodData md = (MethodData) root.getUserObject();
                ps.printf("Thread %s%n", md.thread);
                printTree(root, 0, maxDepth, ps, 0);
            }
        }

        if (linearPrint) {
            for (DefaultMutableTreeNode root : cg.threadCallGraphs.values()) {
                MethodData md = (MethodData) root.getUserObject();
                ps.printf("Thread %s%n", md.thread);
                printLinear(root, ps);
            }

        }
        return true;
    }

    private void printTree(DefaultMutableTreeNode node, int depth, int maxDepth, PrintStream ps, int indent) {
        Enumeration iter = node.children();
        while (iter.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) iter.nextElement();
            TimeMethodData md = (TimeMethodData) child.getUserObject();
            indent(ps, indent);
            printMethodData(md, ps);
            if (child.getChildCount() > 0 && depth < maxDepth) {
                printTree(child, depth++, maxDepth, ps, indent + 2);
            }
        }
    }

    private void printLinear(DefaultMutableTreeNode node, PrintStream ps) {
        Enumeration iter = node.depthFirstEnumeration();
        while (iter.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) iter.nextElement();
            if (child != node) {
                TimeMethodData md = (TimeMethodData) child.getUserObject();
                printMethodData(md, ps);
            }
        }
    }

    private void printMethodData(TimeMethodData md, PrintStream ps) {
        ps.printf("%s, %s %s%n",
                        CallGraphDisplay.TimeFunctions.formatTime(md.entryTimeInfo.wallTime),
                        CallGraphDisplay.TimeFunctions.formatTime(md.exitTimeInfo.wallTime),
                        md.methodName);
    }

    private static void indent(PrintStream ps, int n) {
        for (int i = 0; i < n; i++) {
            ps.print(' ');
        }
    }
}
