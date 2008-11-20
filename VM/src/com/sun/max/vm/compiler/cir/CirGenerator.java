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
package com.sun.max.vm.compiler.cir;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;

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

    private static final Map<ClassMethodActor, CirMethod> _cirCache = new HashMap<ClassMethodActor, CirMethod>();

    public CirGenerator(CirGeneratorScheme cirGeneratorScheme) {
        super(cirGeneratorScheme, "CIR");
        IrObserver irObserver = null;
        final String cirGuiLevel = System.getProperty(CIR_GUI_PROPERTY);
        if (cirGuiLevel != null) {
            final String irObserverClassName = "com.sun.max.vm.compiler.cir.gui.CirObserverAdapter";
            try {
                final Class<?> irObserverClass = Class.forName(irObserverClassName);
                irObserver = (IrObserver) irObserverClass.newInstance();
            } catch (ClassNotFoundException classNotFoundException) {
                ProgramWarning.message("Error creating CIR observer (" + irObserverClassName + "): " + classNotFoundException);
            } catch (InstantiationException instantiationException) {
                ProgramWarning.message("Error creating CIR observer (" + irObserverClassName + "): " + instantiationException);
            } catch (IllegalAccessException illegalAccessException) {
                ProgramWarning.message("Error creating CIR observer (" + irObserverClassName + "): " + illegalAccessException);
            }
        }

        if (irObserver != null && irObserver.attach(this)) {
            addIrObserver(irObserver);
        }
        return;
    }

    public void removeCirMethod(ClassMethodActor classMethodActor) {
        synchronized (_cirCache) {
            _cirCache.remove(classMethodActor);
        }
    }

    public void setCirMethod(ClassMethodActor classMethodActor, CirMethod cirMethod) {
        synchronized (_cirCache) {
            _cirCache.put(classMethodActor, cirMethod);
        }
    }

    public CirMethod getCirMethod(ClassMethodActor classMethodActor) {
        synchronized (_cirCache) {
            return _cirCache.get(classMethodActor);
        }
    }

    @Override
    public CirMethod createIrMethod(ClassMethodActor classMethodActor) {
        synchronized (classMethodActor) {
            CirMethod cirMethod = getCirMethod(classMethodActor);
            if (cirMethod == null) {
                cirMethod = new CirMethod(classMethodActor);
                setCirMethod(classMethodActor, cirMethod);
                notifyAllocation(cirMethod);
            }
            return cirMethod;
        }
    }

    final CirVariableFactory _postTranslationVariableFactory = new CirVariableFactory(-1);

    public final CirVariableFactory postTranslationVariableFactory() {
        return _postTranslationVariableFactory;
    }

}
