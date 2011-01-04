/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.program.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;

/**
 * A factory that permits subclasses of Mutex to b created. To create instances of a {@code Mutex} subclass,
 * the {@link #MUTEX_FACTORY_CLASS_PROPERTY_NAME} property needs to be defined at image build time.
 *
 * @author Mick Jordan
 */
public abstract class MutexFactory {
    /**
     * The name of the system property specifying a subclass of {@link MutexFactory} that is
     * to be instantiated and used at runtime to create Mutex instances. If not specified,
     * then a default factory is used that simply creates NativeMutex instances.
     */
    public static final String MUTEX_FACTORY_CLASS_PROPERTY_NAME = "max.mutex.factory.class";

    private static final MutexFactory instance;

    static {
        final String factoryClassName = System.getProperty(MUTEX_FACTORY_CLASS_PROPERTY_NAME);
        if (factoryClassName == null) {
            instance = new NativeMutexFactory();
        } else {
            try {
                instance = (MutexFactory) Class.forName(factoryClassName).newInstance();
            } catch (Exception exception) {
                throw ProgramError.unexpected("Error instantiating " + factoryClassName, exception);
            }
        }
    }

    /**
     * Subclasses override this method to instantiate objects of a Mutex subclass.
     *
     */
    protected abstract Mutex newMutex();

    /**
     * Subclasses override this method to do per-implementation initialization.
     */
    protected abstract void initializeImpl();

    /**
     * Creates a Mutex object.
     *
     * @return a particular subclass of a Mutex
     */
    public static Mutex create() {
        return instance.newMutex();
    }

    /**
     * Initialize the mutex implementation.
     */
    public static void initialize() {
        instance.initializeImpl();
    }
}
