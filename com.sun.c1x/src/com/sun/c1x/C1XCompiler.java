/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.c1x;

import com.oracle.max.cri.intrinsics.IntrinsicImpl;
import com.oracle.max.criutils.TTY;
import com.sun.c1x.debug.CFGPrinterObserver;
import com.sun.c1x.intrinsics.C1XIntrinsicImplementations;
import com.sun.c1x.observer.CompilationObserver;
import com.sun.c1x.observer.ObservableCompiler;
import com.sun.c1x.stub.CompilerStub;
import com.sun.c1x.target.Backend;
import com.sun.cri.ci.*;
import com.sun.cri.ri.RiRegisterConfig;
import com.sun.cri.ri.RiResolvedMethod;
import com.sun.cri.ri.RiRuntime;
import com.sun.cri.xir.RiXirGenerator;
import com.sun.cri.xir.XirTemplate;
import com.sun.max.platform.*;

import java.lang.management.PlatformManagedObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements the compiler interface for C1X.
 */
public class C1XCompiler extends ObservableCompiler implements CiCompiler {

    public final IntrinsicImpl.Registry intrinsicRegistry = new IntrinsicImpl.Registry();

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

    public CiResult compileMethod(RiResolvedMethod method, int osrBCI, CiStatistics stats, DebugInfoLevel debugInfoLevel) {
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
        final boolean printCompilation = C1XOptions.PrintCompilation && !TTY.isSuppressed();
        if (printCompilation) {
            TTY.println(String.format("C1X %4d %-70s %-45s %-50s ...", index, method.holder().name(), method.name(), method.signature()));
            startTime = System.nanoTime();
        }

        CiResult result = null;
        TTY.Filter filter = new TTY.Filter(C1XOptions.PrintFilter, method);
        C1XCompilation compilation = new C1XCompilation(this, method, osrBCI, stats, debugInfoLevel);
        try {
            result = compilation.compile();
        } finally {
            filter.remove();
            compilation.close();
            if (printCompilation) {
                long time = (System.nanoTime() - startTime) / 100000;
                TTY.println(String.format("C1X %4d %-70s %-45s %-50s | %3d.%dms %5dB", index, "", "", "", time / 10, time % 10, result.targetMethod().targetCodeSize()));
            }
        }

        return result;
    }

    private void init() {
        C1XIntrinsicImplementations.initialize(intrinsicRegistry);

        final List<XirTemplate> xirTemplateStubs = xir.makeTemplates(backend.newXirAssembler());

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
            if (omitStub(id)) {
                continue;
            }
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

    /**
     * The compiler stubs at the moment are generic to target platform. For ARMv7, some stubs have been replaced by
     * runtime calls and hence, they are omitted from adding them to the stubs list.
     *
     * @param id of the stub.
     * @return true if the stub is omitted.
     */
    private boolean omitStub(CompilerStub.Id id) {
        if (Platform.target().arch.isARM() && (id == CompilerStub.Id.fneg || id == CompilerStub.Id.dneg || id == CompilerStub.Id.d2l || id == CompilerStub.Id.f2l)) {
            return true;
        }
        if (Platform.target().arch.isAarch64() && (id == CompilerStub.Id.fneg || id == CompilerStub.Id.dneg)) {
            return true;
        }
        if (Platform.target().arch.isRISCV64() && (id == CompilerStub.Id.fneg || id == CompilerStub.Id.dneg)) {
            return true;
        }

        return false;
    }

    public CompilerStub lookupStub(CompilerStub.Id id) {
        CompilerStub stub = stubs.get(id);
        assert stub != null || (stub == null && omitStub(id)) : "no stub for compiler stub id: " + id;
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
