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

import java.util.ArrayList;
import java.io.PrintStream;

/**
 * All queries on the data must implement this interface.
 *
 * @author Mick Jordan
 *
 */
public interface Query {
    /**
     * Execute the query defined by this instance on the trace data.
     * @param traceRuns list of trace run data
     * @param traceFocus can be set to focus the query on a given trace run or to -1 to force evaluation on all traces
     * @param ps stream to use for output
     * @param args query-specific arguments
     * @return arbitrary value, not currently interpreted
     */
    Object execute(ArrayList<TraceRun> traceRuns, int traceFocus, PrintStream ps, String[] args);
}
