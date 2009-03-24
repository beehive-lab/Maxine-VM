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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.program.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;

/**
 * A factory that permits subclasses of ConditionVariable to b created. To create instances of a {@code ConditionVariable} subclass,
 * the {@link #CONDITIONVARIABLE_FACTORY_CLASS_PROPERTY_NAME} property needs to be defined at image build time.
 *
 * @author Mick Jordan
 */
public abstract class ConditionVariableFactory {
    /**
     * The name of the system property specifying a subclass of {@link ConditionVariableFactory} that is
     * to be instantiated and used at runtime to create ConditionVariable instances. If not specified,
     * then a default factory is used that simply creates NativeConditionVariable instances.
     */
    public static final String CONDITIONVARIABLE_FACTORY_CLASS_PROPERTY_NAME = "max.conditionvariable.factory.class";

    private static final ConditionVariableFactory _instance;

    static {
        final String factoryClassName = System.getProperty(CONDITIONVARIABLE_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            _instance = new NativeConditionVariableFactory();
        } else {
            try {
                _instance = (ConditionVariableFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
    }

    /**
     * Subclasses override this method to instantiate objects of a ConditionVariable subclass.
     *
     */
    protected abstract ConditionVariable newConditionVariable();

    /**
     * Subclasses override this method to do per-implementation initialization.
     */
    protected abstract void initializeImpl();

    /**
     * Creates a ConditionVariable object.
     *
     * @return a particular subclass of a ConditionVariable
     */
    public static ConditionVariable create() {
        return _instance.newConditionVariable();
    }

    /**
     * Initialize the ConditionVariable implementation.
     */
    public static void initialize() {
        _instance.initializeImpl();
    }
}
