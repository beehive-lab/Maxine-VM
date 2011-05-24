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
package com.sun.max.vm.code;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.*;

/**
 * Utility for printing code cache metrics.
 */
public final class CodeCacheMetricsPrinter {

    final boolean verbose;

    public CodeCacheMetricsPrinter(boolean verbose) {
        this.verbose = verbose;
    }

    static class Metrics {
        int n;
        int invocations;
        int bc;
        int mc;
    }

    void add(TreeMap<String, CodeCacheMetricsPrinter.Metrics> metrics, String key, int bc, int mc, int invocations) {
        CodeCacheMetricsPrinter.Metrics m = metrics.get(key);
        if (m == null) {
            m = new Metrics();
            metrics.put(key, m);
        }
        m.n++;
        m.invocations += invocations;
        m.bc += bc;
        m.mc += mc;
    }

    static class Table {
        final int cols;
        final ArrayList<Object> cells = new ArrayList<Object>();

        public Table(Object... headerCols) {
            this.cols = headerCols.length;
            addRow(headerCols);
        }

        void addRow(Object... cols) {
            assert cols.length <= this.cols;
            int i = 0;
            while (i < cols.length) {
                cells.add(cols[i++]);
            }
            while (i++ < this.cols) {
                cells.add(" ");
            }
        }
    }

    public void printTo(PrintStream out) {
        final CodeManager codeManager = Code.getCodeManager();
        final CodeRegion runtimeCodeRegion = codeManager.getRuntimeCodeRegion();
        if (verbose) {
            out.println("Bytecode\tMachineCode\tInvocations\tCodeType\tMethod");
        }
        TreeMap<String, CodeCacheMetricsPrinter.Metrics> metrics = new TreeMap<String, CodeCacheMetricsPrinter.Metrics>();
        TreeMap<String, CodeCacheMetricsPrinter.Metrics> metrics2 = new TreeMap<String, CodeCacheMetricsPrinter.Metrics>();

        for (TargetMethod targetMethod : runtimeCodeRegion.copyOfTargetMethods()) {
            ClassMethodActor methodActor = targetMethod.classMethodActor();
            int bcSize = methodActor == null ? 0 : methodActor.code().length;
            int mcSize = targetMethod.codeLength();
            MethodProfile profile = targetMethod.profile();
            int invocations = 0;
            if (profile != null) {
                invocations = MethodInstrumentation.initialEntryCount - profile.entryCount;
            }
            String type = targetMethod.getClass().getSimpleName() + ":" + targetMethod.flavor;
            add(metrics, type, bcSize, mcSize, invocations);
            if (invocations > 0 && invocations < 10) {
                add(metrics2, type + "#" + invocations, bcSize, mcSize, invocations);
            } else if (invocations >= 10) {
                add(metrics2, type + "#10+", bcSize, mcSize, invocations);
            }

            if (verbose) {
                out.println(String.format("%d\t%d\t%d\t%s\t%s", bcSize, mcSize, invocations, type, targetMethod));
            }
        }

        CodeCacheMetricsPrinter.Metrics t = new Metrics();
        for (Map.Entry<String, CodeCacheMetricsPrinter.Metrics> e1 : metrics.entrySet()) {
            CodeCacheMetricsPrinter.Metrics m = e1.getValue();
            t.n += m.n;
            t.invocations += m.invocations;
            t.bc += m.bc;
            t.mc += m.mc;
        }

        out.println();
        out.println("========== Metrics per code type ==========");
        CodeCacheMetricsPrinter.Table table = new Table("CodeType", "Count", "Bytecode", "MachineCode", "Invocations", "BytecodeToMachineCode");
        table.addRow("------");
        for (Map.Entry<String, CodeCacheMetricsPrinter.Metrics> e1 : metrics.entrySet()) {
            CodeCacheMetricsPrinter.Metrics m = e1.getValue();
            table.addRow(e1.getKey(), pct(m.n, t.n), pct(m.bc, t.bc), pct(m.mc, t.mc), m.invocations, x(m.bc, m.mc));
        }
        table.addRow("------");
        table.addRow("Totals", t.n, t.bc, t.mc);
        out.println(CiUtil.tabulate(table.cells.toArray(), table.cols, 1, 1));

        out.println();
        out.println("========== Metrics per code type and invocation count ==========");
        table = new Table("CodeType#Invocations", "Count", "Bytecode", "MachineCode");
        table.addRow("------");
        for (Map.Entry<String, CodeCacheMetricsPrinter.Metrics> e : metrics2.entrySet()) {
            CodeCacheMetricsPrinter.Metrics m = e.getValue();
            table.addRow(e.getKey(), pct(m.n, t.n), pct(m.bc, t.bc), pct(m.mc, t.mc));
        }
        out.println(CiUtil.tabulate(table.cells.toArray(), table.cols, 1, 1));
    }

    private static String pct(int a, int b) {
        return a + "(" + ((a * 100) / b) + "%)";
    }

    private static String x(int a, int b) {
        if (a == 0 || b == 0) {
            return "";
        }
        return String.format("%.2fx", (float) b / a);
    }

}
