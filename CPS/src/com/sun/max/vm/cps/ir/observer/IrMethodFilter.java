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

import com.sun.max.vm.cps.ir.*;

/**
 * This class implements an IrObserver filter that matches on the name of a method. This is useful
 * for tracing, graphing, or debugging the compilation of a particular method with a known name.
 * <p>
 * To enable the IrMethodFilter, specify the following system properties:
 * <p>
 * <pre>
 *     -Dmax.ir.observer.filters=IrMethodFilter
 *     -Dmax.ir.observer.method=(method name filters separated by ',')
 * </pre>
 * <p>
 * To simplify tracing of all IR's for a particular method, the {@code "max.ir.trace"} property can be used as described
 * {@linkplain IrObserverConfiguration here}.
 *
 * @author Ben L. Titzer
 */
public class IrMethodFilter extends IrObserverAdapter {

    public static final String PROPERTY_FILTER = "max.ir.observer.method";

    private final IrObserver observer;
    private final String[] filters;

    public IrMethodFilter(IrObserver observer, String[] filters) {
        this.filters = filters;
        this.observer = observer;
    }

    public IrMethodFilter(IrObserver observer) {
        this(observer, System.getProperty(PROPERTY_FILTER, "").split(","));
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
        final String signature = irMethod.classMethodActor().format("%H.%n(%p)");
        for (String filter : filters) {
            if (signature.contains(filter)) {
                return true;
            }
        }
        return false;
    }
}
