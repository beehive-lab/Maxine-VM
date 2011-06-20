/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x;

import java.util.*;

import com.sun.c1x.debug.*;
import com.sun.c1x.observer.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.target.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.cri.xir.*;

/**
 * This class implements the compiler interface for C1X.
 */
public class C1XCompiler extends ObservableCompiler {

    public final Map<Object, CompilerStub> stubs = new HashMap<Object, CompilerStub>();

    /**
     * The target that this compiler has been configured for.
     */
    public final CiTarget target;

    /**
     * The runtime that this compiler has been configured for.
     */
    public final RiRuntime runtime;

    /**
     * The XIR generator that lowers Java operations to machine operations.
     */
    public final RiXirGenerator xir;

    private CompilationObserver cfgPrinterObserver;

    /**
     * The ordered set of compiler extensions.
     */
    public List<C1XCompilerExtension> extensions;

    /**
     * The backend that this compiler has been configured for.
     */
    public final Backend backend;

    public final RiRegisterConfig compilerStubRegisterConfig;

    public C1XCompiler(RiRuntime runtime, CiTarget target, RiXirGenerator xirGen, RiRegisterConfig compilerStubRegisterConfig) {
        this.runtime = runtime;
        this.target = target;
        this.xir = xirGen;
        this.compilerStubRegisterConfig = compilerStubRegisterConfig;
        this.backend = Backend.create(target.arch, this);
        init();
    }

    public CiResult compileMethod(RiMethod method, int osrBCI, RiXirGenerator xirGenerator, CiStatistics stats) {
        if (C1XOptions.PrintCFGToFile && cfgPrinterObserver == null) {
            synchronized (this) {
                if (cfgPrinterObserver == null) {
                    cfgPrinterObserver = new CFGPrinterObserver();
                    addCompilationObserver(cfgPrinterObserver);
                }
            }
        }

        long startTime = 0;
        int index = C1XMetrics.CompiledMethods++;
        if (C1XOptions.PrintCompilation) {
            TTY.print(String.format("C1X %4d %-70s %-45s | ", index, method.holder().name(), method.name()));
            startTime = System.nanoTime();
        }

        CiResult result = null;
        TTY.Filter filter = new TTY.Filter(C1XOptions.PrintFilter, method);
        C1XCompilation compilation = new C1XCompilation(this, method, osrBCI, stats);
        try {
            result = compilation.compile();
        } finally {
            filter.remove();
            compilation.close();
            if (C1XOptions.PrintCompilation && !TTY.isSuppressed()) {
                long time = (System.nanoTime() - startTime) / 100000;
                TTY.println(String.format("%3d.%dms", time / 10, time % 10));
            }
        }

        return result;
    }

    private void init() {
        final List<XirTemplate> xirTemplateStubs = xir.buildTemplates(backend.newXirAssembler());

        if (xirTemplateStubs != null) {
            for (XirTemplate template : xirTemplateStubs) {
                TTY.Filter filter = new TTY.Filter(C1XOptions.PrintFilter, template.name);
                try {
                    stubs.put(template, backend.emit(template));
                } finally {
                    filter.remove();
                }
            }
        }

        for (CompilerStub.Id id : CompilerStub.Id.values()) {
            TTY.Filter suppressor = new TTY.Filter(C1XOptions.PrintFilter, id);
            try {
                stubs.put(id, backend.emit(id));
            } finally {
                suppressor.remove();
            }
        }

        if (C1XOptions.PrintCFGToFile) {
            addCompilationObserver(cfgPrinterObserver = new CFGPrinterObserver());
        }
    }

    public CompilerStub lookupStub(CompilerStub.Id id) {
        CompilerStub stub = stubs.get(id);
        assert stub != null : "no stub for compiler stub id: " + id;
        return stub;
    }

    public CompilerStub lookupStub(XirTemplate template) {
        CompilerStub stub = stubs.get(template);
        assert stub != null : "no stub for XirTemplate: " + template;
        return stub;
    }

    public CompilerStub lookupStub(CiRuntimeCall runtimeCall) {
        CompilerStub stub = stubs.get(runtimeCall);
        if (stub == null) {
            stub = backend.emit(runtimeCall);
            stubs.put(runtimeCall, stub);
        }

        assert stub != null : "could not find compiler stub for runtime call: " + runtimeCall;
        return stub;
    }
}
