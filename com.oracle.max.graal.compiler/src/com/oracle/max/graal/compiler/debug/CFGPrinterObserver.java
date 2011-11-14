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
package com.oracle.max.graal.compiler.debug;

import java.io.*;
import java.util.*;

import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.observer.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Observes compilation events and uses {@link CFGPrinter} to produce a control flow graph for the <a
 * href="https://c1visualizer.dev.java.net/">C1 Visualizer</a>.
 */
public class CFGPrinterObserver implements CompilationObserver {

    /**
     * The observation of a single compilation.
     */
    static class Observation {
        final CFGPrinter cfgPrinter;
        final ByteArrayOutputStream buffer;
        public Observation(CiTarget target, RiRuntime runtime) {
            buffer = new ByteArrayOutputStream();
            cfgPrinter = new CFGPrinter(buffer, target, runtime);
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

    private final OutputStream stream;

    public CFGPrinterObserver() {
        this(CompilationPrinter.globalOut());
    }

    public CFGPrinterObserver(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void compilationStarted(CompilationEvent event) {
        if (TTY.isSuppressed()) {
            return;
        }
        Observation o = new Observation(event.getCompilation().compiler.target, event.getCompilation().compiler.runtime);
        o.cfgPrinter.printCompilation(event.getCompilation().method);
        observations.get().push(o);
    }

    @Override
    public void compilationEvent(CompilationEvent event) {
        if (TTY.isSuppressed()) {
            return;
        }
        Observation o = observations.get().peek();
        if (o == null) {
            return;
        }

        String label = event.getLabel();

        boolean cfgprinted = false;
        RiRuntime runtime = o.cfgPrinter.runtime;

        if (event.getAllocator() != null && event.getIntervals() != null) {
            o.cfgPrinter.printIntervals(event.getAllocator(), event.getIntervals(), label);

        } else if (event.getBlockMap() != null) {
            o.cfgPrinter.printCFG(event.getBlockMap(), label);
            o.cfgPrinter.printBytecodes(runtime.disassemble(event.getBlockMap().method));
            cfgprinted = true;

        } else if (event.getCompilation() != null && event.getCompilation().lir() != null) {
            o.cfgPrinter.printCFG(event.getMethod(), label, event.getCompilation().lir(), event.isHIRValid(), event.isLIRValid());
            cfgprinted = true;
        }

        if (event.getTargetMethod() != null) {
            if (cfgprinted) {
                // Avoid duplicate "cfg" section
                label = null;
            }
            o.cfgPrinter.printMachineCode(runtime.disassemble(event.getTargetMethod()), label);
        }
    }

    @Override
    public void compilationFinished(CompilationEvent event) {
        if (TTY.isSuppressed()) {
            return;
        }
        Observation o = observations.get().pop();
        o.cfgPrinter.flush();

        if (stream != null) {
            synchronized (stream) {
                try {
                    stream.write(o.buffer.toByteArray());
                    stream.flush();
                } catch (IOException e) {
                    TTY.println("WARNING: Error writing CFGPrinter output for %s: %s", event.getMethod(), e);
                }
            }
        }
    }
}
