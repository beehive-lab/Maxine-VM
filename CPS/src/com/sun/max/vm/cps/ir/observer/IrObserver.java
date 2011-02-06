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
 * This interface allows instrumenting the compiler's generation and alteration of
 * various intermediate representations of methods.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public interface IrObserver {

    /**
     * This method is called when attaching an IR observer to an IR generator and allows
     * this observer to decide whether it should be attached to the specified IR generator.
     *
     * @param generator the IR generator to which to attach
     * @return true if this observer should be attached to the specified generator; false otherwise
     */
    boolean attach(IrGenerator generator);

    /**
     * This method allows an observer to observe the allocation of an IrMethod.
     *
     * @param irMethod the irMethod just created
     */
    void observeAllocation(IrMethod irMethod);

    /**
     * This method allows an observer to observe an IrMethod just before it is generated.
     *
     * @param irMethod the IrMethod, just before it has been generated
     * @param irGenerator the IrGenerator generating the method
     */
    void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator);

    /**
     * This method allows an observer to observe an IrMethod just after it has been generated.
     *
     * @param irMethod the IrMethod, just after it has been generated
     * @param irGenerator the IrGenerator generating the method
     */
    void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator);

    /**
     * This method allows an observer to observe a change in the internal representation
     * of an IrMethod, e.g. after it has been transformed due to optimization. This method
     * is called before the transformation takes place.
     *
     * @param irMethod the irMethod that has been altered
     * @param context the context (e.g. the optimizer) that has just transformed the method
     * @param transform the transformation applied to this IR
     */
    void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform);

    /**
     * This method allows an observer to observe a change in the internal representation
     * of an IrMethod, e.g. after it has been transformed due to optimization. This method
     * is called just after the transformation takes place.
     *
     * @param irMethod the irMethod that has been altered
     * @param context the context (e.g. the optimizer) that has just transformed the method
     * @param transform the transformation applied to this IR
     */
    void observeAfterTransformation(IrMethod irMethod, Object context, Object transform);

    /**
     * This method allows an observer to do cleanup work. It is called when the RUNNING phase is ending.
     */
    void finish();
}
