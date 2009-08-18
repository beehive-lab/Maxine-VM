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
import com.sun.c1x.target.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class C1XCompiler extends CiCompiler {

    private final Map<GlobalStub, Object> map = new HashMap<GlobalStub, Object>();

    private boolean initialized;

    public C1XCompiler(CiRuntime runtime, Target target) {
        super(runtime, target);
    }

    public CiTargetMethod compileMethod(CiMethod method) {
        return compileMethod(method, -1);
    }

    public CiTargetMethod compileMethod(CiMethod method, int osrBCI) {

        if (!initialized) {
            initialized = true;
            init();
        }

        C1XCompilation compilation = new C1XCompilation(this, target, runtime, method, osrBCI);
        return compilation.compile();
    }

    private void init() {
        final GlobalStubEmitter emitter = target.backend.newGlobalStubEmitter(this);
        for (GlobalStub globalStub : GlobalStub.values()) {
            final CiTargetMethod targetMethod = emitter.emit(globalStub);
            Object result = runtime.registerTargetMethod(targetMethod);
            map.put(globalStub, result);
        }
    }

    public Object lookupGlobalStub(GlobalStub stub) {
        assert map.containsKey(stub);
        return map.get(stub);
    }
}
