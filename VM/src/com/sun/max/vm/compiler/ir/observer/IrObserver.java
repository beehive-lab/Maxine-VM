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

import com.sun.max.vm.compiler.ir.*;

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
     * Gets the most general type of {@linkplain IrMethod IR} for which this observer is specialized.
     */
    Class<? extends IrMethod> observableType();

    /**
     * This method allows an observer to do cleanup work. It is called when the RUNNING phase is ending.
     */
    void finish();
}
