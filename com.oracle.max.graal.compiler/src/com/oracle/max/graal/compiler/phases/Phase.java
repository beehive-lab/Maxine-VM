/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.phases;

import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.observer.*;
import com.oracle.max.graal.compiler.schedule.*;
import com.oracle.max.graal.graph.*;
import com.sun.cri.ci.*;

public abstract class Phase {

    private final String name;
    private final boolean shouldVerify;
    protected final GraalContext context;

    protected Phase(GraalContext context) {
        this.context = context;
        this.name = this.getClass().getSimpleName();
        this.shouldVerify = GraalOptions.Verify;
    }

    protected Phase(GraalContext context, String name) {
        this(context, name, GraalOptions.Verify);
    }

    protected Phase(GraalContext context, String name, boolean shouldVerify) {
        this.name = name;
        this.shouldVerify = shouldVerify;
        this.context = context;
    }

    protected String getDetailedName() {
        return getName();
    }

    public final void apply(Graph graph) {
        apply(graph, true, true);
    }

    public final void apply(Graph graph, boolean plotOnError, boolean plot) {
        assert graph != null && (!shouldVerify || graph.verify());

        int startDeletedNodeCount = graph.getDeletedNodeCount();
        int startNodeCount = graph.getNodeCount();
        boolean shouldFireCompilationEvents = context.isObserved() && this.getClass() != IdentifyBlocksPhase.class && (plot || GraalOptions.PlotVerbose);
//        if (shouldFireCompilationEvents) {
//            compilation.compiler.fireCompilationEvent(new CompilationEvent(compilation, "Before " + getDetailedName(), graph, true, false));
//        }
        context.timers.startScope(getName());
        try {
            run(graph);
        } catch (CiBailout bailout) {
            throw bailout;
        } catch (AssertionError t) {
            if (context.observable.isObserved() && plotOnError) {
                context.observable.fireCompilationEvent(new CompilationEvent(null, "AssertionError in " + getDetailedName(), graph, true, false, true));
            }
            throw t;
        } catch (RuntimeException t) {
            if (context.observable.isObserved() && plotOnError) {
                context.observable.fireCompilationEvent(new CompilationEvent(null, "RuntimeException in " + getDetailedName(), graph, true, false, true));
            }
            throw t;
        } finally {
            context.timers.endScope();
        }

        if (GraalOptions.Meter) {
            int deletedNodeCount = graph.getDeletedNodeCount() - startDeletedNodeCount;
            int createdNodeCount = graph.getNodeCount() - startNodeCount + deletedNodeCount;
            context.metrics.get(getName().concat(".executed")).increment();
            context.metrics.get(getName().concat(".deletedNodes")).increment(deletedNodeCount);
            context.metrics.get(getName().concat(".createdNodes")).increment(createdNodeCount);
        }

        if (shouldFireCompilationEvents && context.timers.currentLevel() < GraalOptions.PlotLevel) {
            context.observable.fireCompilationEvent(new CompilationEvent(null, "After " + getDetailedName(), graph, true, false));
        }

        try {
            assert !shouldVerify || graph.verify();
        } catch (AssertionError e) {
            throw new RuntimeException("Verification error after phase " + this.getDetailedName(), e);
        }
    }

    public final String getName() {
        return name;
    }

    protected abstract void run(Graph graph);
}
