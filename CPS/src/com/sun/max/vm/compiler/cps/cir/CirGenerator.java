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
package com.sun.max.vm.compiler.cps.cir;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.cir.variable.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.compiler.cps.ir.observer.*;

/**
 * Generates CIR.
 *
 * TODO: cache eviction.
 *
 * @author Bernd Mathiske
 */
public abstract class CirGenerator extends IrGenerator<CirGeneratorScheme, CirMethod> {

    /**
     * The name of the system property that, if non-null when a CIR generator is instantiated, enables the CIR visualizer.
     */
    public static final String CIR_GUI_PROPERTY = "max.cir.gui";

    /**
     * The name of the system property that, if non-null when a CIR generator is instantiated, enables the CIR verifier.
     */
    public static final String CIR_VERIFY_PROPERTY = "max.cir.verify";

    private static final Map<ClassMethodActor, CirMethod> cirCache = new HashMap<ClassMethodActor, CirMethod>();

    @HOSTED_ONLY
    private static final Map<ClassMethodActor, CirMethod> hostedCirCache = new HashMap<ClassMethodActor, CirMethod>();

    private CirVerifyingObserver cirVerifyingObserver;

    public CirGenerator(CirGeneratorScheme cirGeneratorScheme) {
        super(cirGeneratorScheme, "CIR");
        final String cirGui = System.getProperty(CIR_GUI_PROPERTY);
        if (cirGui != null) {
            final String irObserverClassName = "com.sun.max.vm.compiler.cir.gui.CirObserverAdapter";
            try {
                final Class<?> irObserverClass = Class.forName(irObserverClassName);
                final IrObserver irObserver = (IrObserver) irObserverClass.newInstance();
                if (irObserver.attach(this)) {
                    addIrObserver(irObserver);
                }
            } catch (ClassNotFoundException classNotFoundException) {
                ProgramWarning.message("Error creating CIR observer (" + irObserverClassName + "): " + classNotFoundException);
            } catch (InstantiationException instantiationException) {
                ProgramWarning.message("Error creating CIR observer (" + irObserverClassName + "): " + instantiationException);
            } catch (IllegalAccessException illegalAccessException) {
                ProgramWarning.message("Error creating CIR observer (" + irObserverClassName + "): " + illegalAccessException);
            }
        }

        final String cirVerify = System.getProperty(CIR_VERIFY_PROPERTY);
        if (cirVerify != null) {
            final CirVerifyingObserver observer = new CirVerifyingObserver();
            if (observer.attach(this)) {
                addIrObserver(observer);
                this.cirVerifyingObserver = observer;
            }
        }
        return;
    }

    /**
     * This method is overridden to ensure that the {@link CirVerifyingObserver} is always the last observer in the
     * list of observers. This means that an invalid CIR graph will be printed if the appropriate tracing
     * observer is attached before the exception is thrown indicating the error in the graph.
     */
    @Override
    public synchronized void addIrObserver(IrObserver observer) {
        if (cirVerifyingObserver != null && irObservers.contains(cirVerifyingObserver)) {
            removeIrObserver(cirVerifyingObserver);
            super.addIrObserver(observer);
            super.addIrObserver(cirVerifyingObserver);
        } else {
            super.addIrObserver(observer);
        }
    }
    
    public void removeCirMethod(ClassMethodActor classMethodActor) {
        synchronized (cirCache) {
            cirCache.remove(classMethodActor);
        }
    }

	public void setCirMethod(ClassMethodActor classMethodActor, CirMethod cirMethod) {
        synchronized (cirCache) {
        	if (MaxineVM.isHosted() && !classMethodActor.isInline()) {
        		hostedCirCache.put(classMethodActor, cirMethod);
        	} else {
        		cirCache.put(classMethodActor, cirMethod);
        	}
        }
    }

    public CirMethod getCirMethod(ClassMethodActor classMethodActor) {
        synchronized (cirCache) {
            CirMethod cirMethod = cirCache.get(classMethodActor);
            if (cirMethod == null && MaxineVM.isHosted()) {
            	cirMethod = hostedCirCache.get(classMethodActor);
            }
			return cirMethod;
        }
    }

    @Override
    public CirMethod createIrMethod(ClassMethodActor classMethodActor) {
        synchronized (cirCache) {
            CirMethod cirMethod = getCirMethod(classMethodActor);
            if (cirMethod == null) {
                cirMethod = new CirMethod(classMethodActor);
                setCirMethod(classMethodActor, cirMethod);
                notifyAllocation(cirMethod);
            }
            return cirMethod;
        }
    }

    final CirVariableFactory postTranslationVariableFactory = new CirVariableFactory(-1);

    public final CirVariableFactory postTranslationVariableFactory() {
        return postTranslationVariableFactory;
    }
}
