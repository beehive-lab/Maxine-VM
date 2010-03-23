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
import com.sun.c1x.xir.*;

/**
 * This class implements the compiler interface for C1X.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class C1XCompiler extends CiCompiler {

    private final Map<Object, GlobalStub> map = new HashMap<Object, GlobalStub>();

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

    /**
     * The backend that this compiler has been configured for.
     */
    public final Backend backend;

    public C1XCompiler(RiRuntime runtime, CiTarget target, RiXirGenerator xirGen) {
        this.runtime = runtime;
        this.target = target;
        this.xir = xirGen;

        this.backend = Backend.create(target.arch, this);
    }

    @Override
    public CiResult compileMethod(RiMethod method, RiXirGenerator xirGenerator) {
        return compileMethod(method, -1, xirGenerator);
    }

    @Override
    public CiResult compileMethod(RiMethod method, int osrBCI, RiXirGenerator xirGenerator) {
        C1XCompilation compilation = new C1XCompilation(this, target, runtime, method, osrBCI);
        return compilation.compile();
    }

    public void init() {
        List<XirTemplate> globalStubs = xir.buildTemplates(backend.newXirAssembler());

        final GlobalStubEmitter emitter = backend.newGlobalStubEmitter();

        for (XirTemplate t : globalStubs) {
            map.put(t, emitter.emit(t, runtime));
        }

        for (GlobalStub.Id id : GlobalStub.Id.values()) {
            map.put(id, emitter.emit(id, runtime));
        }
    }

    public GlobalStub lookupGlobalStub(GlobalStub.Id id) {
        GlobalStub globalStub = map.get(id);
        assert globalStub != null : "no stub for global stub id: " + id;
        return globalStub;
    }

    public GlobalStub lookupGlobalStub(XirTemplate t) {
        GlobalStub globalStub = map.get(t);
        assert globalStub != null : "no stub for XirTemplate: " + t;
        return globalStub;
    }

    public GlobalStub lookupGlobalStub(CiRuntimeCall rtcall) {
        GlobalStub globalStub = map.get(rtcall);
        if (globalStub == null) {
            globalStub = backend.newGlobalStubEmitter().emit(rtcall, runtime);
            map.put(rtcall, globalStub);
        }

        assert globalStub != null : "could not find global stub for runtime call: " + rtcall;
        return globalStub;
    }
}
