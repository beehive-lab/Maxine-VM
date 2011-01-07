/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.observer;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.cps.ir.*;

/**
 * This class implements an IR observer that dispatches observation requests for a method's IR based on a set of
 * configured IR trace observers and the IR trace observer type {@linkplain IrMethod#irTraceObserverType() specific} to
 * the IR method type.
 *
 * @author Doug Simon
 */
public class IrTraceObserverDispatcher extends IrObserverAdapter {

    private final Map<Class<? extends IrTraceObserver>, IrTraceObserver> observersByType;
    private final IrTraceObserver defaultObserver = new IrTraceObserver();

    public IrTraceObserverDispatcher() {
        observersByType = new HashMap<Class<? extends IrTraceObserver>, IrTraceObserver>();
    }

    private IrTraceObserver observerFor(IrMethod irMethod) {
        final Class<? extends IrTraceObserver> irTraceObserverType = irMethod.irTraceObserverType();
        if (irTraceObserverType != null && !irTraceObserverType.equals(defaultObserver.getClass())) {
            IrTraceObserver irTraceObserver = observersByType.get(irTraceObserverType);
            if (irTraceObserver == null) {
                try {
                    irTraceObserver = irTraceObserverType.newInstance();
                    observersByType.put(irTraceObserverType, irTraceObserver);
                } catch (InstantiationException e) {
                    throw ProgramError.unexpected("Could not initialize IR observer implemented by " + irTraceObserverType, e);
                } catch (IllegalAccessException e) {
                    throw ProgramError.unexpected("Could not initialize IR observer implemented by " + irTraceObserverType, e);
                }
            }
            return irTraceObserver;
        }
        return defaultObserver;
    }

    @Override
    public void observeAllocation(IrMethod irMethod) {
        observerFor(irMethod).observeAllocation(irMethod);
    }

    @Override
    public void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        observerFor(irMethod).observeBeforeGeneration(irMethod, irGenerator);
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        observerFor(irMethod).observeAfterGeneration(irMethod, irGenerator);
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        observerFor(irMethod).observeBeforeTransformation(irMethod, context, transform);
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        observerFor(irMethod).observeAfterTransformation(irMethod, context, transform);
    }
}
