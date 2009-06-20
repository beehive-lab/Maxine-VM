/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.ir.observer;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;

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
