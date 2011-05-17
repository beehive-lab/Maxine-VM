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
package com.sun.c1x.debug;

import java.io.*;
import java.util.*;

import com.sun.c1x.observer.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Observes compilation events and uses {@link CFGPrinter} to produce a control flow graph for the <a
 * href="https://c1visualizer.dev.java.net/">C1 Visualizer</a>. This observer is thread-safe and
 * supports re-entrant compilation.
 */
public class CFGPrinterObserver implements CompilationObserver {

    /**
     * The observation of a single compilation.
     */
    static class Observation {
        final CFGPrinter cfgPrinter;
        final ByteArrayOutputStream buffer;
        public Observation(CiTarget target) {
            buffer = new ByteArrayOutputStream();
            cfgPrinter = new CFGPrinter(buffer, target);
        }
    }

    /**
     * A thread local stack of {@link Observation}s is used to support thread-safety and re-entrant compilation.
     */
    private ThreadLocal<LinkedList<Observation>> observations = new ThreadLocal<LinkedList<Observation>>() {
        @Override
        protected java.util.LinkedList<Observation> initialValue() {
            return new LinkedList<Observation>();
        }
    };

    public CFGPrinterObserver() {
    }

    @Override
    public void compilationStarted(CompilationEvent event) {
        Observation o = new Observation(event.getCompilation().target);
        o.cfgPrinter.printCompilation(event.getCompilation().method);
        observations.get().push(o);
    }

    @Override
    public void compilationEvent(CompilationEvent event) {
        Observation o = observations.get().peek();
        String label = event.getLabel();

        if (event.getAllocator() != null && event.getIntervals() != null) {
            o.cfgPrinter.printIntervals(event.getAllocator(), event.getIntervals(), label);
        }

        boolean cfgprinted = false;

        if (event.getBlockMap() != null && event.getCodeSize() >= 0) {
            o.cfgPrinter.printCFG(event.getMethod(), event.getBlockMap(), event.getCodeSize(), label, event.isHIRValid(), event.isLIRValid());
            cfgprinted = true;
        }

        if (event.getStartBlock() != null) {
            o.cfgPrinter.printCFG(event.getStartBlock(), label, event.isHIRValid(), event.isLIRValid());
            cfgprinted = true;
        }

        if (event.getTargetMethod() != null) {
            if (cfgprinted) {
                // Avoid duplicate "cfg" section
                label = null;
            }

            RiRuntime runtime = event.getCompilation().runtime;
            o.cfgPrinter.printMachineCode(runtime.disassemble(event.getTargetMethod()), label);
        }
    }

    @Override
    public void compilationFinished(CompilationEvent event) {
        Observation o = observations.get().pop();
        o.cfgPrinter.flush();

        OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
        if (cfgFileStream != null) {
            synchronized (cfgFileStream) {
                try {
                    cfgFileStream.write(o.buffer.toByteArray());
                } catch (IOException e) {
                    TTY.println("WARNING: Error writing CFGPrinter output for %s to disk: %s", event.getMethod(), e);
                }
            }
        }
    }
}
