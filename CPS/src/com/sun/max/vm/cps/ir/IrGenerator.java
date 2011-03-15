/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.runtime.*;

public abstract class IrGenerator<CompilerScheme_Type extends RuntimeCompiler, IrMethod_Type extends IrMethod> {

    private long numberOfCompilations = 0L;

    private final CompilerScheme_Type compilerScheme;
    public final String irName;
    public final Class<IrMethod_Type> irMethodType;

    @RESET
    protected transient LinkedList<IrObserver> irObservers;

    protected IrGenerator(CompilerScheme_Type compilerScheme, String irName) {
        this.compilerScheme = compilerScheme;
        this.irName = irName;
        Class<Class<IrMethod_Type>> type = null;
        try {
            irMethodType = Utils.cast(type, getClass().getMethod("createIrMethod", ClassMethodActor.class).getReturnType());
        } catch (Exception e) {
            throw FatalError.unexpected("Error getting exact IrMethod type for compiler", e);
        }
    }

    public CompilerScheme_Type compilerScheme() {
        return compilerScheme;
    }

    /**
     * Obtain the representation of the given class method actor in this IR,
     * creating the result object if necessary.
     *
     * Note: this call does NOT generate IR for the method body.
     *
     * @param classMethodActor a method actor
     * @return an object that represents the class method actor in this IR
     */
    public abstract IrMethod_Type createIrMethod(ClassMethodActor classMethodActor);

    /**
     * Generate and install IR for the method body of the given IR method.
     */
    protected abstract void generateIrMethod(IrMethod_Type irMethod, boolean install);

    public final void makeIrMethod(final IrMethod_Type irMethod, boolean install) {
        if (!irMethod.isGenerated()) {
            notifyBeforeGeneration(irMethod);
            generateIrMethod(irMethod, install);
            notifyAfterGeneration(irMethod);
            irMethod.cleanup();
        }
    }

    public final void notifyAllocation(IrMethod irMethod) {
        if (irObservers != null) {
            synchronized (this) {
                for (IrObserver observer : irObservers) {
                    observer.observeAllocation(irMethod);
                }
            }
        }
    }

    public final void notifyBeforeGeneration(IrMethod irMethod) {
        if (irObservers != null) {
            synchronized (this) {
                for (IrObserver observer : irObservers) {
                    observer.observeBeforeGeneration(irMethod, this);
                }
            }
        }
    }

    public final void notifyAfterGeneration(IrMethod irMethod) {
        if (irObservers != null) {
            synchronized (this) {
                for (IrObserver observer : irObservers) {
                    observer.observeAfterGeneration(irMethod, this);
                }
            }
        }
        numberOfCompilations++;
    }

    public final void notifyBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irObservers != null) {
            synchronized (this) {
                for (IrObserver observer : irObservers) {
                    observer.observeBeforeTransformation(irMethod, context, transform);
                }
            }
        }
    }

    public final void notifyAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irObservers != null) {
            synchronized (this) {
                for (IrObserver observer : irObservers) {
                    observer.observeAfterTransformation(irMethod, context, transform);
                }
            }
        }
    }

    public final void notifyAfterFinish() {
        if (irObservers != null) {
            synchronized (this) {
                for (IrObserver observer : irObservers) {
                    observer.finish();
                }
            }
        }
    }

    /**
     * This method provides support for adding observers to the allocation and generation of IR methods by this
     * generator. The observer will be notified in the event of allocating a new IrMethod, and before and after
     * generation.
     *
     * @param observer the new observer to add to this generator
     */
    public synchronized void addIrObserver(IrObserver observer) {
        if (irObservers == null) {
            irObservers = new LinkedList<IrObserver>();
        }
        irObservers.add(observer);
    }

    /**
     * This method provides support for removing an observer that is currently attached
     * to this generator. The specified observer will no longer be notified of events
     * regarding IrMethods produced and generated by this generator.
     * @param observer the observer to remove from this IR generator
     */
    public synchronized void removeIrObserver(IrObserver observer) {
        if (irObservers != null) {
            irObservers.remove(observer);
            if (irObservers.size() == 0) {
                irObservers = null; // remove the list if it becomes empty
            }
        }
    }

    /**
     * Return a completed IR method representation.
     * Obtain the representation of the given class method actor in this IR,
     * creating the result object if necessary.
     * If IR for the method body has not been generated and installed yet, do so.
     *
     * Note: This method does not synchronize on {@code classMethodActor}; it is expected
     * that the caller ensures that at most one thread is ever attempting to compile a method.
     * Synchronizing {@code classMethodActor} here can cause deadlock due to the fact
     * that {@link ClassMethodActor#compilee()} is synchronized.
     */
    public final IrMethod_Type makeIrMethod(ClassMethodActor classMethodActor, boolean install) {
        final IrMethod_Type irMethod = createIrMethod(classMethodActor);
        makeIrMethod(irMethod, install);
        return irMethod;
    }

    public long numberOfCompilations() {
        return numberOfCompilations;
    }

    @Override
    public String toString() {
        return irName + "Generator";
    }
}
