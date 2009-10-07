/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.target.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.xir.*;

/**
 * This class implements the compiler interface for C1X.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class C1XCompiler extends CiCompiler {

    private final Map<GlobalStub, Object> map = new HashMap<GlobalStub, Object>();
    private final Map<CiRuntimeCall, Object> runtimeCallStubs = new HashMap<CiRuntimeCall, Object>();

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
    public final XirGenerator xir;

    /**
     * The backend that this compiler has been configured for.
     */
    public final Backend backend;


    public C1XCompiler(RiRuntime runtime, CiTarget target, XirGenerator xirGen) {
        this.runtime = runtime;
        this.target = target;
        this.xir = xirGen;

        // TODO: Remove this fixed wiring to X86
        assert target.arch instanceof AMD64;
        this.backend = new X86Backend(this);
        init();
    }

    @Override
    public CiResult compileMethod(RiMethod method, XirGenerator xirGenerator) {
        return compileMethod(method, -1, xirGenerator);
    }

    @Override
    public CiResult compileMethod(RiMethod method, int osrBCI, XirGenerator xirGenerator) {
        C1XCompilation compilation = new C1XCompilation(this, target, runtime, method, osrBCI);
        return compilation.compile();
    }

    private void init() {

        xir.buildTemplates(backend.newXirAssembler());

        final GlobalStubEmitter emitter = backend.newGlobalStubEmitter();
        for (GlobalStub globalStub : GlobalStub.values()) {
            final CiTargetMethod targetMethod = emitter.emit(globalStub);
            Object result = runtime.registerTargetMethod(targetMethod, globalStub.toString());
            map.put(globalStub, result);
        }
    }

    public Object lookupGlobalStub(GlobalStub stub) {
        assert map.containsKey(stub);
        return map.get(stub);
    }

    public Object lookupGlobalStub(CiRuntimeCall dest) {

        if (!runtimeCallStubs.containsKey(dest)) {

            final GlobalStubEmitter emitter = backend.newGlobalStubEmitter();
            final CiTargetMethod targetMethod = emitter.emitRuntimeStub(dest);
            Object result = runtime.registerTargetMethod(targetMethod, dest.toString());
            runtimeCallStubs.put(dest, result);
        }


        assert runtimeCallStubs.containsKey(dest);
        return runtimeCallStubs.get(dest);

    }
}
