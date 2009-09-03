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
package com.sun.max.vm.compiler.ir;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.ir.observer.*;

public abstract class IrGenerator<CompilerScheme_Type extends RuntimeCompilerScheme, IrMethod_Type extends IrMethod> {

    private long numberOfCompilations = 0L;

    private final CompilerScheme_Type compilerScheme;
    private final String irName;

    @RESET
    protected transient LinkedList<IrObserver> irObservers;

    protected IrGenerator(CompilerScheme_Type compilerScheme, String irName) {
        this.compilerScheme = compilerScheme;
        this.irName = irName;
    }

    public final String irName() {
        return irName;
    }

    public CompilerScheme_Type compilerScheme() {
        return compilerScheme;
    }

    public boolean isCrossCompiling() {
        return !compilerScheme.vmConfiguration().equals(VMConfiguration.host());
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
    protected abstract void generateIrMethod(IrMethod_Type irMethod);

    public final void makeIrMethod(final IrMethod_Type irMethod) {
        if (!irMethod.isGenerated()) {
            MaxineVM.usingTarget(new Runnable() {
                public void run() {
                    notifyBeforeGeneration(irMethod);
                    generateIrMethod(irMethod);
                    notifyAfterGeneration(irMethod);
                    irMethod.cleanup();
                }
            });
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
    public final IrMethod_Type makeIrMethod(ClassMethodActor classMethodActor) {
        final IrMethod_Type irMethod = createIrMethod(classMethodActor);
        makeIrMethod(irMethod);
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
