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
package com.sun.max.vm.compiler.cps.ir.observer;

import com.sun.max.lang.*;
import com.sun.max.vm.compiler.cps.ir.*;

/**
 * This class implements an IrObserver filter that matches on the level of the IR (e.g. CIR). This is useful
 * for tracing, graphing, or debugging the compilation of a particular level of IR, especially with a filter
 * that selects a particular method.
 * <p>
 * To enable the IrMethodFilter, specify the following system properties:
 * <p>
 * -Dmax.ir.observer.filters=IrLevelFilter
 * -Dmax.ir.observer.levels=(levels)
 * <p>
 * The second option specifies a comma-separated list of IR levels to trace. For example, "cir,dir" would
 * enable tracing of CirMethods and DirMethods only.
 *
 * @author Ben L. Titzer
 */
public class IrLevelFilter extends IrObserverAdapter {

    private final IrObserver observer;
    private final String[] filter;

    public IrLevelFilter(IrObserver observer) {
        this.filter = System.getProperty("max.ir.observer.levels", "").split(",");
        // capitalize the name of all the IRs.
        for (int i = 0; i < filter.length; i++) {
            if (filter[i].length() > 0) {
                filter[i] = Strings.capitalizeFirst(filter[i], false);
            }
        }
        this.observer = observer;
    }

    @Override
    public void observeAllocation(IrMethod irMethod) {
        if (match(irMethod)) {
            observer.observeAllocation(irMethod);
        }
    }

    @Override
    public void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (match(irMethod)) {
            observer.observeBeforeGeneration(irMethod, irGenerator);
        }
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (match(irMethod)) {
            observer.observeAfterGeneration(irMethod, irGenerator);
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (match(irMethod)) {
            observer.observeBeforeTransformation(irMethod, context, transform);
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (match(irMethod)) {
            observer.observeAfterTransformation(irMethod, context, transform);
        }
    }

    @Override
    public void finish() {
        observer.finish();
    }

    private boolean match(IrMethod irMethod) {
        final String name = irMethod.getClass().getSimpleName();
        for (String f : this.filter) {
            if (name.startsWith(f)) {
                return true;
            }
        }
        return false;
    }
}
